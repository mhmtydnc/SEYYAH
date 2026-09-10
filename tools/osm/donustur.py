
import csv
import json
import math
import re
import unicodedata
from collections import Counter, defaultdict

GIRDI, CIKTI, DENETIM = "poi.geojsonseq", "poi.csv", "birlesenler.csv"


FILTRE = [
    ("historic", {"castle", "ruins", "monument", "archaeological_site", "memorial",
                  "city_gate", "fort", "tower"}),
    ("tourism",  {"museum", "zoo", "theme_park", "aquarium", "gallery", "viewpoint",
                  "artwork", "picnic_site"}),
    ("natural",  {"waterfall", "beach", "cave_entrance", "peak", "hot_spring"}),
    ("leisure",  {"park", "nature_reserve", "garden"}),
    ("tourism",  {"attraction"}),
    ("amenity",  {"place_of_worship", "restaurant", "cafe", "fuel", "toilets", "parking"}),
]


def tarihi_mi(e):
    return ("historic" in e or "wikidata" in e or "tourism" in e
            or any(k.startswith("heritage") for k in e))


def kategori_tur(e):
    for anahtar, degerler in FILTRE:
        d = e.get(anahtar)
        if d not in degerler:          # anahtarın varlığı değil, DEĞERİN filtrede olması önemli
            continue
        if anahtar != "amenity":
            return d, "gezi"
        if d == "parking":
            return d, "destek"
        if d == "place_of_worship":    # tarihi cami → gezi, mahalle camisi → yol üstü namaz molası
            return d, "gezi" if tarihi_mi(e) else "mola"
        return d, "mola"
    return None, None




def _halka(h):
    """Kapalı halkanın alan merkezi ve alanı (shoelace). Köşe ortalaması, köşelerin sık olduğu
    tarafa kayar; bu gerçek ağırlık merkezini verir. İlk köşeye göre hesaplanır ki büyük
    koordinatların çarpımında hassasiyet kaybolmasın."""
    x0, y0 = h[0][0], h[0][1]
    a = cx = cy = 0.0
    for p, q in zip(h, h[1:]):
        x1, y1, x2, y2 = p[0] - x0, p[1] - y0, q[0] - x0, q[1] - y0
        c = x1 * y2 - x2 * y1
        a += c
        cx += (x1 + x2) * c
        cy += (y1 + y2) * c
    if abs(a) < 1e-14:                 # dejenere halka → köşe ortalaması
        return sum(p[0] for p in h) / len(h), sum(p[1] for p in h) / len(h), 0.0
    return x0 + cx / (3 * a), y0 + cy / (3 * a), abs(a) / 2


def _cizgi_ortasi(c):

    boylar = [math.dist(p[:2], q[:2]) for p, q in zip(c, c[1:])]
    kalan = sum(boylar) / 2
    for (p, q), l in zip(zip(c, c[1:]), boylar):
        if l > 0 and l >= kalan:
            t = kalan / l
            return p[0] + (q[0] - p[0]) * t, p[1] + (q[1] - p[1]) * t
        kalan -= l
    return c[0][0], c[0][1]


def merkez(g):
    t, c = g["type"], g["coordinates"]
    if t == "Point":
        return c[0], c[1]
    if t == "LineString":
        if len(c) >= 4 and c[0] == c[-1]:   # kapalı çizgi aslında bir alan
            return _halka(c)[:2]
        return _cizgi_ortasi(c)
    if t == "Polygon":
        return _halka(c[0])[:2]
    if t == "MultiPolygon":                 # en büyük parçanın merkezi
        return max((_halka(p[0]) for p in c), key=lambda r: r[2])[:2]
    return None


def osm_anahtari(ham):
    t, n = ham[0], int(ham[1:])
    # Bazı osmium sürümleri alanları 'a' + (2*way_id | 2*rel_id+1) olarak yazar. Asıl kimliğe
    # çevrilmezse aynı yer hem 'w' hem 'a' olarak iki kez girer.
    if t == "a":
        return ("w" if n % 2 == 0 else "r"), n // 2
    return t, n

# ---------------------------------------------------------------- okuma


def normalize(ad):
    # "İvaz Paşa Camii" ve "İvazpaşa Camii" → "ivazpasacamii"
    s = unicodedata.normalize("NFKD", ad.replace("İ", "I").replace("ı", "i")).lower()
    return re.sub(r"[^a-z0-9]", "", s)


def oku():
    tekil, sayac = {}, Counter()
    with open(GIRDI, encoding="utf-8") as f:
        for satir in f:
            satir = satir.strip()      # -r verilmezse baştaki RS karakteri de burada gider
            if not satir:
                continue
            sayac["satir"] += 1
            o = json.loads(satir)
            e = o.get("properties") or {}
            ad = (e.get("name") or "").strip()
            kategori, tur = kategori_tur(e)
            if not ad or not kategori:
                continue
            m = merkez(o["geometry"])
            if not m:
                continue
            anahtar = osm_anahtari(o["id"])
            alan = o["geometry"]["type"] in ("Polygon", "MultiPolygon")
            onceki = tekil.get(anahtar)
            if onceki:
                sayac["ayni_nesne"] += 1
                if onceki["alan"] or not alan:     # poligondan gelen merkez daha doğru
                    continue
            tekil[anahtar] = {
                "osm_type": anahtar[0], "osm_id": anahtar[1], "ad": ad, "norm": normalize(ad),
                "kategori": kategori, "tur": tur, "lon": m[0], "lat": m[1], "alan": alan,
                "etiketler": e,
                "ucret": e.get("charge") or e.get("fee"),   # charge "20 TL" der, fee yes/no
                "calisma_saatleri": e.get("opening_hours"),
                "wikidata_id": e.get("wikidata"),
                "website": e.get("website") or e.get("contact:website"),
            }
    return list(tekil.values()), sayac

# ---------------------------------------------------------------- tekrar birleştirme

ESIK_M = {
    # Büyük alanlar: node ile poligon merkezi arası yüzlerce metre olabilir
    "castle": 400, "fort": 400, "archaeological_site": 500, "ruins": 300, "park": 300,
    "nature_reserve": 800, "theme_park": 400, "zoo": 400, "beach": 500,
    # Mola: bölünmüş yolun iki yakasındaki aynı markalı istasyonlar birleşmesin
    "fuel": 60, "restaurant": 80, "cafe": 80, "toilets": 40,
}
VARSAYILAN_M = 150
AYNI_TIP_M = 30       # aynı tip + aynı kategori iki kayıt ancak bu kadar yakınsa aynı yerdir
HUCRE = 0.02          # derece (~1,5-2 km); en büyük eşikten büyük olmalı


def mesafe_m(a, b):
    x = math.radians(b["lon"] - a["lon"]) * math.cos(math.radians((a["lat"] + b["lat"]) / 2))
    y = math.radians(b["lat"] - a["lat"])
    return 6_371_000 * math.hypot(x, y)


def ayni_yer_mi(a, b, m):
    if a["wikidata_id"] and b["wikidata_id"] and a["wikidata_id"] != b["wikidata_id"]:
        return False                   # iki ayrı Wikidata varlığı: isim benzerliği tesadüf
    if a["osm_type"] == b["osm_type"] and a["kategori"] == b["kategori"]:
        return m <= AYNI_TIP_M         # iki "Çay Bahçesi" node'u büyük ihtimalle iki ayrı yer
    return m <= max(ESIK_M.get(a["kategori"], VARSAYILAN_M),
                    ESIK_M.get(b["kategori"], VARSAYILAN_M))


def zenginlik(k):
    # Son eleman eşitlikte küçük (eski) osm_id'yi seçer → kazanan aydan aya değişmez,
    # places.id'ler sabit kalır, route_candidates boşuna silinmez.
    return (bool(k["wikidata_id"]), len(k["etiketler"]), k["alan"], -k["osm_id"])


def hucre(k):
    return int(k["lat"] // HUCRE), int(k["lon"] // HUCRE)


def grup(k):
    # Camiler birleştirmede hep gezi grubunda: tipik tekrar, historic etiketsiz node (mola) +
    # historic=yes bina (gezi). Ayrı gruplarda olsalar asla eşleşmezlerdi. Asıl tür birleştirme
    # sonrası, birleşmiş etiketlere göre yeniden hesaplanır.
    return "gezi" if k["kategori"] == "place_of_worship" else k["tur"]


def tekrarlari_birlestir(kayitlar):
    """Açgözlü birleştirme: en zengin kayıt kazanan olur ve SADECE kendisine doğrudan yakın
    kayıtları yutar. Zincirleme yok: A~B, B~C olsa bile C, A'ya uzaksa ayrı kalır."""
    kova = defaultdict(list)
    for k in kayitlar:
        if k["tur"] != "destek" and k["norm"]:
            kova[(k["norm"], grup(k), *hucre(k))].append(k)

    kayitlar.sort(key=zenginlik, reverse=True)
    islendi, sonuc, denetim = set(), [], []
    for ana in kayitlar:
        if id(ana) in islendi:
            continue
        islendi.add(id(ana))
        sonuc.append(ana)
        if ana["tur"] == "destek" or not ana["norm"]:
            continue
        hy, hx = hucre(ana)
        for dy in (-1, 0, 1):
            for dx in (-1, 0, 1):
                for diger in kova.get((ana["norm"], grup(ana), hy + dy, hx + dx), ()):
                    if id(diger) in islendi:
                        continue
                    m = mesafe_m(ana, diger)
                    if not ayni_yer_mi(ana, diger, m):
                        continue
                    islendi.add(id(diger))
                    for alan in ("ucret", "calisma_saatleri", "wikidata_id", "website"):
                        if not ana[alan] and diger[alan]:
                            ana[alan] = diger[alan]
                    ana["etiketler"] = {**diger["etiketler"], **ana["etiketler"]}
                    denetim.append((ana["ad"], ana["kategori"], diger["kategori"],
                                    f"{ana['osm_type']}{ana['osm_id']}",
                                    f"{diger['osm_type']}{diger['osm_id']}", round(m)))
    for k in sonuc:
        if k["kategori"] == "place_of_worship":
            k["tur"] = "gezi" if tarihi_mi(k["etiketler"]) else "mola"
    return sonuc, denetim

# ---------------------------------------------------------------- önem skoru

KATEGORI_TABAN = {
    "museum": 25, "castle": 25, "archaeological_site": 25, "fort": 20, "waterfall": 20,
    "ruins": 15, "cave_entrance": 15, "hot_spring": 15, "zoo": 15, "aquarium": 15,
    "theme_park": 15, "viewpoint": 12, "city_gate": 12, "attraction": 10, "tower": 10,
    "beach": 10, "nature_reserve": 10, "monument": 8, "peak": 8, "gallery": 8,
    "garden": 6, "place_of_worship": 5, "picnic_site": 4, "park": 3, "memorial": 3,
    "artwork": 2,
}


def onem_skoru(k):
    e = k["etiketler"]
    s = KATEGORI_TABAN.get(k["kategori"], 0)
    if e.get("wikidata"):
        s += 25
    if e.get("wikipedia"):
        s += 15
    if any(t.startswith("heritage") for t in e):
        s += 10
    if e.get("tourism") == "attraction" and k["kategori"] != "attraction":
        s += 8
    if "historic" in e and k["kategori"] == "place_of_worship":
        s += 8                                             # tarihi camiler
    if e.get("image") or e.get("wikimedia_commons"):
        s += 5
    s += 2 * min(sum(t.startswith("name:") for t in e), 5)  # çok dilli ad = bilinirlik
    if e.get("opening_hours"):
        s += 2
    s += min(len(e), 30) // 3
    return min(s, 100)

# ---------------------------------------------------------------- yazma


def yaz(kayitlar, denetim):
    with open(CIKTI, "w", newline="", encoding="utf-8") as f:
        w = csv.writer(f)
        w.writerow(["osm_type", "osm_id", "ad", "kategori", "tur", "lon", "lat", "ucret",
                    "calisma_saatleri", "wikidata_id", "website", "onem_skoru"])
        for k in kayitlar:
            w.writerow([k["osm_type"], k["osm_id"], k["ad"], k["kategori"], k["tur"],
                        f"{k['lon']:.7f}", f"{k['lat']:.7f}", k["ucret"],
                        k["calisma_saatleri"], k["wikidata_id"], k["website"], k["onem_skoru"]])
    with open(DENETIM, "w", newline="", encoding="utf-8-sig") as f:   # -sig: Excel Türkçe'yi açsın
        w = csv.writer(f)
        w.writerow(["ad", "kazanan_kategori", "kaybeden_kategori", "kazanan", "kaybeden", "metre"])
        w.writerows(sorted(denetim, key=lambda r: -r[5]))                # en uzaklar en üstte


if __name__ == "__main__":
    kayitlar, sayac = oku()
    print(f"{sayac['satir']:>8}  geojson satırı")
    print(f"{sayac['ayni_nesne']:>8}  aynı OSM nesnesinin ikinci geometrisi atlandı")
    print(f"{len(kayitlar):>8}  tekil OSM nesnesi (adlı ve filtreye uyan)")

    kayitlar, denetim = tekrarlari_birlestir(kayitlar)
    print(f"{len(denetim):>8}  kayıt birleştirildi → {len(kayitlar)} yer")
    for kat, n in Counter(r[2] for r in denetim).most_common(8):
        print(f"{'':>10}{n:>7}  {kat}")

    for k in kayitlar:
        k["onem_skoru"] = onem_skoru(k)
    kayitlar.sort(key=lambda k: (k["osm_type"], k["osm_id"]))
    yaz(kayitlar, denetim)

    print("Tür dağılımı:")
    for tur, n in sorted(Counter(k["tur"] for k in kayitlar).items()):
        print(f"{'':>10}{n:>7}  {tur}")
    print(f"→ {CIKTI}, {DENETIM}")