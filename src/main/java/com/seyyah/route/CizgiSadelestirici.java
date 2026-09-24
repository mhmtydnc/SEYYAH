package com.seyyah.route;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

// Haritaya gönderilen rota çizgisini Douglas-Peucker ile sadeleştirir. 700 km'lik bir rota ~6.000 nokta
// (her yanıtta rota başına ~150 KB); harita için ~10 m tolerans görünüşte fark yaratmaz.
// Koridor sorgusu tam geometriyi (WKT) kullanmaya devam eder.
final class CizgiSadelestirici {

    // Derece cinsinden; enlem yönünde ~10 m
    static final double VARSAYILAN_TOLERANS = 0.0001;

    private CizgiSadelestirici() {
    }

    // Noktalar [boylam, enlem]; ilk ve son nokta her zaman korunur
    static List<List<Double>> sadelestir(List<List<Double>> noktalar, double tolerans) {
        int n = noktalar.size();
        if (n < 3) {
            return noktalar;
        }
        boolean[] tut = new boolean[n];
        tut[0] = true;
        tut[n - 1] = true;

        // Özyineleme yerine yığın: uzun rotada derin özyineleme yığın taşırabilir
        Deque<int[]> araliklar = new ArrayDeque<>();
        araliklar.push(new int[]{0, n - 1});
        while (!araliklar.isEmpty()) {
            int[] aralik = araliklar.pop();
            int bas = aralik[0], son = aralik[1];
            double enUzak = -1;
            int enUzakIndeks = -1;
            for (int i = bas + 1; i < son; i++) {
                double d = dogruyaUzaklik(noktalar.get(i), noktalar.get(bas), noktalar.get(son));
                if (d > enUzak) {
                    enUzak = d;
                    enUzakIndeks = i;
                }
            }
            if (enUzak > tolerans) {
                tut[enUzakIndeks] = true;
                araliklar.push(new int[]{bas, enUzakIndeks});
                araliklar.push(new int[]{enUzakIndeks, son});
            }
        }

        List<List<Double>> sonuc = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            if (tut[i]) {
                sonuc.add(noktalar.get(i));
            }
        }
        return sonuc;
    }

    // p noktasının a-b doğru parçasına düzlemsel uzaklığı (derece)
    private static double dogruyaUzaklik(List<Double> p, List<Double> a, List<Double> b) {
        double px = p.get(0), py = p.get(1), ax = a.get(0), ay = a.get(1), bx = b.get(0), by = b.get(1);
        double dx = bx - ax, dy = by - ay;
        double uzunlukKare = dx * dx + dy * dy;
        if (uzunlukKare == 0) {
            return Math.hypot(px - ax, py - ay);
        }
        double t = Math.max(0, Math.min(1, ((px - ax) * dx + (py - ay) * dy) / uzunlukKare));
        return Math.hypot(px - (ax + t * dx), py - (ay + t * dy));
    }
}
