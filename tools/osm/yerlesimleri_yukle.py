"""
veri/yerlesimler.csv dosyasını yerlesimler tablosuna yükler (varsa günceller). Tablo Flyway V5 ile
oluşur; önce uygulama bir kez o veritabanına bağlanmış olmalı.

Kullanım (depo kökünden):
    DB_URL=jdbc:postgresql://host:5432/seyyah_dev?sslmode=require DB_USER=... DB_PASS=... \
        python tools/osm/yerlesimleri_yukle.py

Gereksinimler:
    pip install "psycopg[binary]"
"""

import csv
import os
import sys
from urllib.parse import urlsplit, parse_qs

import psycopg

GIRDI = "veri/yerlesimler.csv"
KOLONLAR = ["osm_type", "osm_id", "ad", "tur", "il", "ilce", "nufus", "onem", "enlem", "boylam"]


def baglanti_ayarlari():
    url, kullanici, sifre = os.getenv("DB_URL"), os.getenv("DB_USER"), os.getenv("DB_PASS")
    if not all([url, kullanici, sifre]):
        sys.exit("HATA: DB_URL, DB_USER ve DB_PASS ortam değişkenleri gerekli.")
    if not url.startswith("jdbc:postgresql://"):
        sys.exit("HATA: DB_URL jdbc:postgresql:// ile başlamalı.")
    parca = urlsplit(url[len("jdbc:"):])
    sorgu = parse_qs(parca.query)
    # Sözlükle bağlanılır: şifredeki boşluk/özel karakter bağlantı metnini bozamaz
    return {
        "host": parca.hostname, "port": parca.port or 5432, "dbname": parca.path.lstrip("/"),
        "user": kullanici, "password": sifre,
        "sslmode": sorgu.get("sslmode", ["require"])[0], "connect_timeout": 15,
    }


def satirlar():
    with open(GIRDI, encoding="utf-8", newline="") as f:
        for s in csv.DictReader(f):
            yield (s["osm_type"], int(s["osm_id"]), s["ad"], s["tur"], s["il"] or None, s["ilce"] or None,
                   int(s["nufus"]) if s["nufus"] else None, int(s["onem"]),
                   float(s["enlem"]), float(s["boylam"]))


def ana():
    ayar = baglanti_ayarlari()
    print(f"Bağlanılıyor: {ayar['host']}/{ayar['dbname']} ({ayar['user']})")
    with psycopg.connect(**ayar) as baglanti, baglanti.cursor() as imlec:
        imlec.execute("""
            CREATE TEMP TABLE gecici_yerlesimler (
                osm_type CHAR(1), osm_id BIGINT, ad TEXT, tur TEXT, il TEXT, ilce TEXT,
                nufus INT, onem INT, enlem FLOAT8, boylam FLOAT8
            ) ON COMMIT DROP
        """)
        with imlec.copy(f"COPY gecici_yerlesimler ({', '.join(KOLONLAR)}) FROM STDIN") as kopya:
            for satir in satirlar():
                kopya.write_row(satir)

        imlec.execute("""
            INSERT INTO yerlesimler (osm_type, osm_id, ad, tur, il, ilce, nufus, onem, konum)
            SELECT osm_type, osm_id, ad, tur, il, ilce, nufus, onem,
                   ST_SetSRID(ST_MakePoint(boylam, enlem), 4326)::geography
            FROM gecici_yerlesimler
            ON CONFLICT (osm_type, osm_id) DO UPDATE SET
                ad = EXCLUDED.ad, tur = EXCLUDED.tur, il = EXCLUDED.il, ilce = EXCLUDED.ilce,
                nufus = EXCLUDED.nufus, onem = EXCLUDED.onem, konum = EXCLUDED.konum
        """)
        print(f"{imlec.rowcount} yerleşim eklendi/güncellendi.")
        imlec.execute("ANALYZE yerlesimler")


if __name__ == "__main__":
    ana()
