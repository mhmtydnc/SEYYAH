"""
Yerleşimleri (şehir, kasaba, köy, mahalle) OSM PBF'den çıkarır; il ve ilçesini idari sınırlardan bulur.

Kullanım (depo kökünden):
    python tools/osm/yerlesimler.py
    → veri/turkey-latest.osm.pbf okunur, veri/yerlesimler.csv yazılır

Gereksinimler:
    pip install osmium shapely

OSM'de is_in:province etiketi yerleşimlerin yalnızca ~%2'sinde var; bu yüzden il/ilçe, noktanın
hangi admin_level=4 (il) ve admin_level=6 (ilçe) sınırının içinde kaldığına bakılarak atanır.
Aynı adlı yüzlerce köy ("Yeniköy") ancak böyle ayırt edilebilir.
"""

import csv
import osmium
import shapely
from shapely import STRtree, Point

GIRDI = "veri/turkey-latest.osm.pbf"
CIKTI = "veri/yerlesimler.csv"

ONEM = {"city": 100, "town": 60, "suburb": 30, "quarter": 20, "village": 10, "neighbourhood": 5}
IDARI_SEVIYE = {"4": "il", "6": "ilce"}


def sayi(s):
    if not s:
        return None
    s = s.replace(" ", "").replace(".", "").replace(",", "")
    return int(s) if s.isdigit() else None


def oku():
    """Tek geçişte yerleşim node'larını ve il/ilçe sınır alanlarını toplar."""
    wkb = osmium.geom.WKBFactory()
    yerlesimler, alanlar = [], {"il": [], "ilce": []}

    # with_areas: çoklu poligonlar için gereken ikinci geçişi ve node konumlarını pyosmium yönetir
    isleyici = (osmium.FileProcessor(GIRDI)
                .with_areas(osmium.filter.TagFilter(("boundary", "administrative")))
                .with_filter(osmium.filter.KeyFilter("place", "boundary")))

    for nesne in isleyici:
        etiket = nesne.tags
        if nesne.is_area():
            seviye = IDARI_SEVIYE.get(etiket.get("admin_level"))
            ad = etiket.get("name:tr") or etiket.get("name")
            if not seviye or not ad or etiket.get("boundary") != "administrative":
                continue
            try:
                alanlar[seviye].append((shapely.from_wkb(wkb.create_multipolygon(nesne)), ad))
            except RuntimeError:        # kapanmayan/bozuk sınır; o ilçe atlanır
                pass
        elif nesne.is_node():
            tur = etiket.get("place")
            ad = etiket.get("name") or etiket.get("name:tr")
            if tur not in ONEM or not ad:
                continue
            il = etiket.get("is_in:province") or etiket.get("addr:province") or ""
            yerlesimler.append({
                "osm_type": "n", "osm_id": nesne.id, "ad": ad, "tur": tur, "il": il, "ilce": "",
                "nufus": sayi(etiket.get("population")), "onem": ONEM[tur],
                "enlem": nesne.location.lat, "boylam": nesne.location.lon,
            })
    return yerlesimler, alanlar


def ata(yerlesimler, alanlar, alan_adi):
    geometriler = [g for g, _ in alanlar]
    agac = STRtree(geometriler)
    noktalar = [Point(y["boylam"], y["enlem"]) for y in yerlesimler]
    # Toplu sorgu: (nokta indeksi, alan indeksi) çiftleri
    nokta_i, alan_i = agac.query(noktalar, predicate="within")
    bulunan = {}
    for n, a in zip(nokta_i, alan_i):
        bulunan.setdefault(n, a)
    for i, y in enumerate(yerlesimler):
        if i in bulunan:
            y[alan_adi] = alanlar[bulunan[i]][1]
        elif not y[alan_adi]:
            # Kıyıdaki ya da sınır çizgisinin hemen dışındaki nokta: en yakın alan
            y[alan_adi] = alanlar[agac.nearest(noktalar[i])][1]
    return len(bulunan)


def ana():
    print(f"{GIRDI} okunuyor (birkaç dakika sürer)...")
    yerlesimler, alanlar = oku()
    print(f"{len(yerlesimler)} yerleşim, {len(alanlar['il'])} il ve {len(alanlar['ilce'])} ilçe sınırı bulundu")
    if len(alanlar["il"]) < 70:
        raise SystemExit("İl sınırları eksik okunmuş; alan birleştirme çalışmamış olabilir")

    for seviye in ("il", "ilce"):
        icinde = ata(yerlesimler, alanlar[seviye], seviye)
        print(f"  {seviye}: {icinde} yerleşim sınır içinde, kalanlar en yakın alana atandı")

    alanlar_sirali = ["osm_type", "osm_id", "ad", "tur", "il", "ilce", "nufus", "onem", "enlem", "boylam"]
    with open(CIKTI, "w", newline="", encoding="utf-8") as f:
        w = csv.DictWriter(f, fieldnames=alanlar_sirali)
        w.writeheader()
        for y in yerlesimler:
            w.writerow({**y, "nufus": "" if y["nufus"] is None else y["nufus"],
                        "enlem": f"{y['enlem']:.7f}", "boylam": f"{y['boylam']:.7f}"})
    print(f"{CIKTI} yazıldı")


if __name__ == "__main__":
    ana()
