"""
Yerleşimleri veritabanına yükler.
DB_URL (jdbc:postgresql://...), DB_USER, DB_PASS ortam değişkenlerini kullanır.
"""

import csv
import os
import sys
import psycopg

GIRDI = "veri/yerlesimler.csv"

def parse_jdbc_url(url):
    # jdbc:postgresql://localhost:5432/seyyah?options...
    if not url.startswith("jdbc:postgresql://"):
        raise ValueError(f"Desteklenmeyen DB_URL: {url}")
    
    parts = url[18:].split("?", 1)
    host_port_db = parts[0]
    
    if "/" in host_port_db:
        host_port, dbname = host_port_db.split("/", 1)
    else:
        host_port = host_port_db
        dbname = ""
        
    if ":" in host_port:
        host, port = host_port.split(":", 1)
    else:
        host = host_port
        port = "5432"
        
    return host, port, dbname

def ana():
    db_url = os.getenv("DB_URL")
    db_user = os.getenv("DB_USER")
    db_pass = os.getenv("DB_PASS")
    
    if not all([db_url, db_user, db_pass]):
        print("HATA: DB_URL, DB_USER ve DB_PASS ortam değişkenleri gerekli.")
        sys.exit(1)
        
    host, port, dbname = parse_jdbc_url(db_url)
    conninfo = f"host={host} port={port} dbname={dbname} user={db_user} password={db_pass}"
    
    print(f"Bağlanılıyor: {host}:{port} db={dbname} user={db_user}")
    
    with psycopg.connect(conninfo) as conn:
        with conn.cursor() as cur:
            cur.execute("""
                CREATE TEMP TABLE tmp_yerlesimler (
                    osm_type CHAR(1),
                    osm_id BIGINT,
                    ad TEXT,
                    tur TEXT,
                    il TEXT,
                    nufus INT,
                    onem INT,
                    enlem FLOAT,
                    boylam FLOAT
                ) ON COMMIT DROP;
            """)
            
            with open(GIRDI, "r", encoding="utf-8") as f:
                reader = csv.reader(f)
                next(reader) # skip header
                
                with cur.copy("COPY tmp_yerlesimler FROM STDIN") as copy:
                    for row in reader:
                        osm_type, osm_id, ad, tur, il, nufus, onem, enlem, boylam = row
                        copy.write_row((
                            osm_type, 
                            int(osm_id), 
                            ad, 
                            tur, 
                            il if il else None, 
                            int(nufus) if nufus else None, 
                            int(onem), 
                            float(enlem), 
                            float(boylam)
                        ))
            
            # Upsert
            cur.execute("""
                INSERT INTO yerlesimler (osm_type, osm_id, ad, tur, il, nufus, onem, konum)
                SELECT 
                    osm_type, osm_id, ad, tur, il, nufus, onem, 
                    ST_SetSRID(ST_MakePoint(boylam, enlem), 4326)::geography
                FROM tmp_yerlesimler
                ON CONFLICT (osm_type, osm_id) DO UPDATE SET
                    ad = EXCLUDED.ad,
                    tur = EXCLUDED.tur,
                    il = EXCLUDED.il,
                    nufus = EXCLUDED.nufus,
                    onem = EXCLUDED.onem,
                    konum = EXCLUDED.konum
            """)
            
            rowcount = cur.rowcount
            print(f"{rowcount} kayıt eklendi/güncellendi.")

if __name__ == "__main__":
    ana()
