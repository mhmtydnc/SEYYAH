package com.seyyah.rotalarim;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface KayitliRotaRepository extends JpaRepository<KayitliRota, Long> {

    List<KayitliRota> findByKullaniciIdOrderByOlusturulmaDesc(Long kullaniciId);

    Optional<KayitliRota> findByIdAndKullaniciId(Long id, Long kullaniciId);
}
