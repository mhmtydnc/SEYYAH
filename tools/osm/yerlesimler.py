"""
Yerleşim verilerini (şehir, ilçe, köy, mahalle vs.) OSM PBF dosyasından çıkarır.

Kullanım:
    python yerlesimler.py
    (veri/turkey-latest.osm.pbf okur, veri/yerlesimler.csv yazar)

Gereksinimler:
    pip install osmium
"""

import csv
import osmium

GIRDI = "veri/turkey-latest.osm.pbf"
CIKTI = "veri/yerlesimler.csv"

YERLESIM_TUR = {"city", "town", "village", "suburb", "quarter", "neighbourhood"}
ONEM_SKORU = {
    "city": 100,
    "town": 60,
    "suburb": 30,
    "quarter": 20,
    "village": 10,
    "neighbourhood": 5
}

def parse_int(s):
    try:
        if s:
            s = s.replace(" ", "").replace(".", "").replace(",", "")
            return int(s)
    except:
        pass
    return None

def il_bul(tags):
    il = tags.get("is_in:province")
    if not il:
        il = tags.get("addr:province")
    if not il:
        is_in = tags.get("is_in")
        if is_in:
            il = is_in.split(",")[0].strip()
    return il or ""

class YerlesimHandler(osmium.SimpleHandler):
    def __init__(self):
        osmium.SimpleHandler.__init__(self)
        self.kayitlar = []

    def node(self, n):
        tags = n.tags
        tur = tags.get("place")
        if tur not in YERLESIM_TUR:
            return
            
        ad = tags.get("name") or tags.get("name:tr")
        if not ad:
            return
            
        il = il_bul(tags)
        nufus = parse_int(tags.get("population"))
        onem = ONEM_SKORU.get(tur, 0)
        
        self.kayitlar.append([
            "n", n.id, ad, tur, il, nufus if nufus is not None else "", onem,
            f"{n.location.lat:.7f}", f"{n.location.lon:.7f}"
        ])

def ana():
    print(f"{GIRDI} okunuyor...")
    handler = YerlesimHandler()
    handler.apply_file(GIRDI)
    
    print(f"{len(handler.kayitlar)} yerleşim bulundu. {CIKTI} yazılıyor...")
    
    with open(CIKTI, "w", newline="", encoding="utf-8") as f:
        w = csv.writer(f)
        w.writerow(["osm_type", "osm_id", "ad", "tur", "il", "nufus", "onem", "enlem", "boylam"])
        w.writerows(handler.kayitlar)
        
    print("Bitti.")

if __name__ == "__main__":
    ana()
