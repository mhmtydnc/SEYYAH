"""
`places` tablosundaki tekrar eden (duplicate) kayıtları bulur ve temizler.

Kural:
- Aynı tur, aynı ad (büyük/küçük harf ve aksan duyarsız)
- Ad uzunluğu en az 3 karakter
- Mesafe <= 300m. İstisna: Adı 1 veya 2 kelimeden oluşan ve kategorisi
  park, place_of_worship, parking veya toilets olan yerlerde mesafe <= 60m.
  (Adaylardan en az biri bu şarta uyuyorsa 60m sınırı uygulanır.)

Tutulacak kaydın seçimi:
1. En yüksek onem_skoru
2. Eşitse way/relation (w, r), node'a (n) tercih edilir
3. Eşitse dolu alan sayısı (wikidata_id, website, calisma_saatleri, ucret) fazla olan
4. Eşitse en küçük id

Tutulan kayda, silinen kayıtların dolu alanları aktarılır (eğer tutulan kayıtta o alan boşsa)
ve onem_skoru kümedeki en yüksek onem_skoru'na eşitlenir.

Kullanım:
    python tools/osm/tekrar_temizle.py           # Kuru çalıştırma (dry-run)
    python tools/osm/tekrar_temizle.py --uygula  # Veritabanına uygular
"""
import argparse
import os
import sys
from collections import defaultdict
from urllib.parse import urlsplit, parse_qs

import psycopg

def baglanti_ayarlari():
    url, kullanici, sifre = os.getenv("DB_URL"), os.getenv("DB_USER"), os.getenv("DB_PASS")
    if not all([url, kullanici, sifre]):
        sys.exit("HATA: DB_URL, DB_USER ve DB_PASS ortam değişkenleri gerekli.")
    if not url.startswith("jdbc:postgresql://"):
        sys.exit("HATA: DB_URL jdbc:postgresql:// ile başlamalı.")
    parca = urlsplit(url[len("jdbc:"):])
    sorgu = parse_qs(parca.query)
    return {
        "host": parca.hostname, "port": parca.port or 5432, "dbname": parca.path.lstrip("/"),
        "user": kullanici, "password": sifre,
        "sslmode": sorgu.get("sslmode", ["require"])[0], "connect_timeout": 15,
    }

def istisna_durumu_uygun(ad, kategori):
    kelime_sayisi = len(ad.split())
    istisna_kategoriler = {"park", "place_of_worship", "parking", "toilets"}
    return kelime_sayisi < 3 and kategori in istisna_kategoriler

def mesafesi_uygun(mesafe, yer1, yer2):
    # yer = {..., 'ad': '...', 'kategori': '...', 'osm_type': 'n|w|r'}
    istisna1 = istisna_durumu_uygun(yer1['ad'], yer1['kategori'])
    istisna2 = istisna_durumu_uygun(yer2['ad'], yer2['kategori'])
    # Asıl tekrar deseni aynı yerin hem node hem way olarak girilmesi. Aynı tipte iki kayıt kısa bir
    # cins isim taşıyorsa ("Korugan", "Lahit", "Sarnıç") büyük olasılıkla yan yana duran ayrı yapılardır.
    ayni_tip_cins_isim = (yer1.get('osm_type') == yer2.get('osm_type')
                          and len(yer1['ad'].split()) <= 2)

    sinir = 60 if (istisna1 or istisna2 or ayni_tip_cins_isim) else 300
    return mesafe <= sinir

class UnionFind:
    def __init__(self):
        self.parent = {}
    def find(self, i):
        if self.parent.setdefault(i, i) == i:
            return i
        self.parent[i] = self.find(self.parent[i])
        return self.parent[i]
    def union(self, i, j):
        root_i = self.find(i)
        root_j = self.find(j)
        if root_i != root_j:
            self.parent[root_i] = root_j

def dolu_alan_sayisi(yer):
    sayi = 0
    for alan in ['wikidata_id', 'website', 'calisma_saatleri', 'ucret']:
        val = yer.get(alan)
        if val is not None and str(val).strip() != "":
            sayi += 1
    return sayi

def tip_skoru(osm_type):
    return 1 if osm_type in ('w', 'r') else 0

def secim_kriteri(yer):
    # (onem_skoru, tip_skoru, dolu_alan_sayisi, -id)
    return (
        yer.get('onem_skoru', 0) or 0,
        tip_skoru(yer.get('osm_type', 'n')),
        dolu_alan_sayisi(yer),
        -yer['id']
    )

def tutulacak_kaydi_sec(kume_yerleri):
    return max(kume_yerleri, key=secim_kriteri)

def bos_alanlari_doldur(tutulan, kume_yerleri):
    # tutulan objesinin kopyasını döndüreceğiz
    yeni_tutulan = dict(tutulan)
    alanlar = ['wikidata_id', 'website', 'calisma_saatleri', 'ucret']
    
    max_onem = max((y.get('onem_skoru', 0) or 0) for y in kume_yerleri)
    yeni_tutulan['onem_skoru'] = max_onem
    
    for alan in alanlar:
        val = yeni_tutulan.get(alan)
        if val is None or str(val).strip() == "":
            # Diğerlerinden bulmaya çalış
            for yer in kume_yerleri:
                v = yer.get(alan)
                if v is not None and str(v).strip() != "":
                    yeni_tutulan[alan] = v
                    break
    return yeni_tutulan

def ana():
    parser = argparse.ArgumentParser()
    parser.add_argument('--uygula', action='store_true', help="Veritabanına uygula (update/delete)")
    args = parser.parse_args()

    ayar = baglanti_ayarlari()
    if args.uygula:
        print(f"Bağlanılıyor: {ayar['host']}/{ayar['dbname']} ({ayar['user']})")
    else:
        print("Kuru çalıştırma (dry-run).")
    
    with psycopg.connect(**ayar) as baglanti, baglanti.cursor(row_factory=psycopg.rows.dict_row) as imlec:
        # Aday çiftleri ve mesafeleri getir (aynı tur, aynı normalleştirilmiş isim, uzunluk >=3, ve mesafe <= 300)
        # 300 metre geniş sınır, 60m kuralı pythonda filtrelenir.
        sorgu = """
        WITH adaylar AS (
            SELECT p.id, p.osm_type, p.osm_id, p.ad, p.kategori, p.tur,
                   p.onem_skoru, p.wikidata_id, p.website, p.calisma_saatleri, p.ucret, p.konum,
                   lower(f_unaccent(p.ad)) as norm_ad
            FROM places p
            WHERE length(p.ad) >= 3
        )
        SELECT a1.id as id1, a2.id as id2,
               ST_Distance(a1.konum, a2.konum) as mesafe,
               a1.osm_type as type1, a1.osm_id as osmid1, a1.ad as ad1, a1.kategori as kat1,
               a1.onem_skoru as onem1, a1.wikidata_id as wd1, a1.website as web1, a1.calisma_saatleri as saat1, a1.ucret as ucret1,
               a2.osm_type as type2, a2.osm_id as osmid2, a2.ad as ad2, a2.kategori as kat2,
               a2.onem_skoru as onem2, a2.wikidata_id as wd2, a2.website as web2, a2.calisma_saatleri as saat2, a2.ucret as ucret2,
               a1.tur as tur1, a1.norm_ad as norm1
        FROM adaylar a1
        JOIN adaylar a2 ON a1.tur = a2.tur AND a1.norm_ad = a2.norm_ad AND a1.id < a2.id
        WHERE ST_DWithin(a1.konum, a2.konum, 300);
        """
        imlec.execute(sorgu)
        sonuclar = imlec.fetchall()
        
        yerler = {}
        uf = UnionFind()
        
        for r in sonuclar:
            yer1 = {
                'id': r['id1'], 'osm_type': r['type1'], 'osm_id': r['osmid1'], 'ad': r['ad1'],
                'kategori': r['kat1'], 'onem_skoru': r['onem1'], 'wikidata_id': r['wd1'],
                'website': r['web1'], 'calisma_saatleri': r['saat1'], 'ucret': r['ucret1'], 'tur': r['tur1']
            }
            yer2 = {
                'id': r['id2'], 'osm_type': r['type2'], 'osm_id': r['osmid2'], 'ad': r['ad2'],
                'kategori': r['kat2'], 'onem_skoru': r['onem2'], 'wikidata_id': r['wd2'],
                'website': r['web2'], 'calisma_saatleri': r['saat2'], 'ucret': r['ucret2'], 'tur': r['tur1']
            }
            
            if not mesafesi_uygun(r['mesafe'], yer1, yer2):
                continue
                
            yerler[r['id1']] = yer1
            yerler[r['id2']] = yer2
            uf.union(r['id1'], r['id2'])
            
            # Mesafeyi kaydet, istatistik için
            if 'mesafeler' not in yerler[r['id1']]: yerler[r['id1']]['mesafeler'] = []
            if 'mesafeler' not in yerler[r['id2']]: yerler[r['id2']]['mesafeler'] = []
            yerler[r['id1']]['mesafeler'].append(r['mesafe'])
            yerler[r['id2']]['mesafeler'].append(r['mesafe'])

        kumeler = defaultdict(list)
        for y_id in yerler:
            kumeler[uf.find(y_id)].append(yerler[y_id])
            
        # Sadece 1'den fazla elemanı olanlar gerçek kümelerdir.
        kumeler = {k: v for k, v in kumeler.items() if len(v) > 1}
        
        silinecekler_toplam = 0
        guncellenecekler_toplam = 0
        
        ozet_listesi = []
        
        for k_id, kume in kumeler.items():
            tutulan = tutulacak_kaydi_sec(kume)
            yeni_tutulan = bos_alanlari_doldur(tutulan, kume)
            
            silinecekler = [y for y in kume if y['id'] != tutulan['id']]
            silinecekler_toplam += len(silinecekler)
            
            # Güncelleme gerekli mi?
            degisti_mi = False
            guncelleme_sql = []
            parametreler = []
            
            for alan in ['onem_skoru', 'wikidata_id', 'website', 'calisma_saatleri', 'ucret']:
                if yeni_tutulan.get(alan) != tutulan.get(alan):
                    degisti_mi = True
                    guncelleme_sql.append(f"{alan} = %s")
                    parametreler.append(yeni_tutulan.get(alan))
                    
            if degisti_mi:
                guncellenecekler_toplam += 1
                yeni_tutulan['guncelleme_gerekli'] = True
                yeni_tutulan['guncelleme_parametreleri'] = parametreler
                yeni_tutulan['guncelleme_sql'] = guncelleme_sql
            else:
                yeni_tutulan['guncelleme_gerekli'] = False
                
            # Özet listesine ekle
            max_onem = yeni_tutulan['onem_skoru']
            mesafeler = [m for y in kume if 'mesafeler' in y for m in y['mesafeler']]
            max_mesafe = max(mesafeler) if mesafeler else 0
            avg_mesafe = sum(mesafeler) / len(mesafeler) if mesafeler else 0
            
            ad = tutulan['ad']
            kategoriler = list(set([y.get('kategori') for y in kume if y.get('kategori')]))
            tipler = list(set([y.get('osm_type') for y in kume if y.get('osm_type')]))
            
            ozet_listesi.append({
                'max_onem': max_onem,
                'ad': ad,
                'kategoriler': kategoriler,
                'tipler': tipler,
                'max_mesafe': max_mesafe,
                'avg_mesafe': avg_mesafe,
                'kume_boyutu': len(kume),
                'tutulan_id': tutulan['id'],
                'silinecekler': [y['id'] for y in silinecekler],
                'yeni_tutulan': yeni_tutulan
            })

        ozet_listesi.sort(key=lambda x: x['max_onem'] or 0, reverse=True)
        
        if not args.uygula:
            print(f"Toplam {len(kumeler)} küme bulundu, {silinecekler_toplam} satır silinecek.")
            print("\nEn önemli 20 küme özeti:")
            for ozet in ozet_listesi[:20]:
                print(f"Ad: {ozet['ad']}, Max Önem: {ozet['max_onem']}, Kategoriler: {ozet['kategoriler']}, Tipler: {ozet['tipler']}")
                print(f"  Küme boyutu: {ozet['kume_boyutu']}, Max mesafe: {ozet['max_mesafe']:.2f}m, Ort mesafe: {ozet['avg_mesafe']:.2f}m")
        else:
            # İlk SELECT örtük bir transaction açtı: transaction() burada yalnızca savepoint olurdu ve
            # autocommit'i değiştirmek hata verip her şeyi geri alırdı. Hepsi aynı transaction'da, sonda tek commit.
            for ozet in ozet_listesi:
                if ozet['silinecekler']:
                    imlec.execute("DELETE FROM places WHERE id = ANY(%s)", (ozet['silinecekler'],))
                y_t = ozet['yeni_tutulan']
                if y_t.get('guncelleme_gerekli'):
                    sql = f"UPDATE places SET {', '.join(y_t['guncelleme_sql'])} WHERE id = %s"
                    imlec.execute(sql, y_t['guncelleme_parametreleri'] + [ozet['tutulan_id']])
            imlec.execute("ANALYZE places")
            baglanti.commit()
            print(f"İşlem tamamlandı: {silinecekler_toplam} satır silindi, {guncellenecekler_toplam} tutulan satır güncellendi.")

if __name__ == "__main__":
    ana()
