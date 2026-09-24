package com.seyyah.detay;

// null yanıt = hiç denenmedi ya da hata (önbelleğe yazılmaz, sonra tekrar denenir).
// detay == null = arandı ama eşleşme/puan yok; bu da önbelleğe yazılır ki her görüntülemede kota harcanmasın.
public record GoogleServisYaniti(
        String placeId,
        GoogleDetay detay
) {
    static GoogleServisYaniti eslesmeYok() {
        return new GoogleServisYaniti(null, null);
    }
}
