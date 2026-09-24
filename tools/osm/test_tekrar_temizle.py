import unittest
from tekrar_temizle import (
    istisna_durumu_uygun, mesafesi_uygun, UnionFind, secim_kriteri,
    tutulacak_kaydi_sec, bos_alanlari_doldur, dolu_alan_sayisi
)

class TestTekrarTemizle(unittest.TestCase):
    def test_istisna_durumu_uygun(self):
        self.assertTrue(istisna_durumu_uygun("Gül Parkı", "park"))
        self.assertTrue(istisna_durumu_uygun("Ulu Cami", "place_of_worship"))
        self.assertFalse(istisna_durumu_uygun("Çok Uzun İsimli Park", "park"))
        self.assertFalse(istisna_durumu_uygun("Gül Parkı", "restaurant"))

    def test_mesafesi_uygun(self):
        y1 = {'ad': 'Gül Parkı', 'kategori': 'park'}
        y2 = {'ad': 'Park', 'kategori': 'park'}
        y3 = {'ad': 'Uzun İsimli Büyük Park', 'kategori': 'park'}
        y4 = {'ad': 'Restaurant', 'kategori': 'restaurant'}
        
        # İstisna var, sınır 60. 50 <= 60 (True), 70 <= 60 (False)
        self.assertTrue(mesafesi_uygun(50, y1, y2))
        self.assertFalse(mesafesi_uygun(70, y1, y2))
        
        # İstisna yok, sınır 300.
        self.assertTrue(mesafesi_uygun(250, y3, y4))
        self.assertFalse(mesafesi_uygun(310, y3, y4))
        
        # Biri istisna, diğeri değil. Sınır 60.
        self.assertTrue(mesafesi_uygun(50, y1, y3))
        self.assertFalse(mesafesi_uygun(70, y1, y3))

    def test_ayni_tipte_kisa_cins_isim_60_metre(self):
        # Gelibolu'daki yan yana ayrı sığınaklar birleşmemeli
        k1 = {'ad': 'Korugan', 'kategori': 'ruins', 'osm_type': 'n'}
        k2 = {'ad': 'Korugan', 'kategori': 'ruins', 'osm_type': 'n'}
        self.assertFalse(mesafesi_uygun(96, k1, k2))
        # Aynı yer hem node hem way: 300 m geçerli
        c1 = {'ad': 'Kapalı Çarşı', 'kategori': 'attraction', 'osm_type': 'w'}
        c2 = {'ad': 'Kapalı Çarşı', 'kategori': 'museum', 'osm_type': 'n'}
        self.assertTrue(mesafesi_uygun(160, c1, c2))
        # Uzun özel ad, aynı tip: 300 m geçerli
        d1 = {'ad': 'Aşağı Düden Şelalesi', 'kategori': 'attraction', 'osm_type': 'n'}
        self.assertTrue(mesafesi_uygun(250, d1, dict(d1)))

    def test_union_find(self):
        uf = UnionFind()
        uf.union(1, 2)
        uf.union(2, 3)
        self.assertEqual(uf.find(1), uf.find(3))
        self.assertNotEqual(uf.find(1), uf.find(4))

    def test_dolu_alan_sayisi(self):
        y = {'wikidata_id': 'Q123', 'website': '', 'calisma_saatleri': None, 'ucret': 'yes'}
        self.assertEqual(dolu_alan_sayisi(y), 2)

    def test_tutulacak_kaydi_sec(self):
        y1 = {'id': 10, 'onem_skoru': 50, 'osm_type': 'n', 'wikidata_id': 'Q1'}
        y2 = {'id': 11, 'onem_skoru': 60, 'osm_type': 'n'}
        y3 = {'id': 12, 'onem_skoru': 60, 'osm_type': 'w'}
        y4 = {'id': 9,  'onem_skoru': 60, 'osm_type': 'w'}
        
        # y2 > y1 (skor)
        self.assertEqual(tutulacak_kaydi_sec([y1, y2])['id'], 11)
        # y3 > y2 (tip)
        self.assertEqual(tutulacak_kaydi_sec([y2, y3])['id'], 12)
        # y4 > y3 (id daha küçük)
        self.assertEqual(tutulacak_kaydi_sec([y3, y4])['id'], 9)
        
        y5 = {'id': 8, 'onem_skoru': 60, 'osm_type': 'w', 'wikidata_id': 'Q2'}
        # y5 > y4 (dolu alan)
        self.assertEqual(tutulacak_kaydi_sec([y4, y5])['id'], 8)

    def test_bos_alanlari_doldur(self):
        kume = [
            {'id': 1, 'onem_skoru': 10, 'wikidata_id': 'Q1', 'website': None},
            {'id': 2, 'onem_skoru': 50, 'wikidata_id': None, 'website': 'http'},
            {'id': 3, 'onem_skoru': 30, 'wikidata_id': 'Q3', 'website': None}
        ]
        tutulan = kume[0]
        yeni = bos_alanlari_doldur(tutulan, kume)
        
        self.assertEqual(yeni['onem_skoru'], 50)
        self.assertEqual(yeni['wikidata_id'], 'Q1') # kendi değeri korunur
        self.assertEqual(yeni['website'], 'http') # diğerinden alınır

if __name__ == '__main__':
    unittest.main()
