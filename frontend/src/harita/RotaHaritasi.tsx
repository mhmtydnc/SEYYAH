import { useEffect, useRef } from 'react'
import { CircleMarker, MapContainer, Marker, Popup, Polyline, TileLayer, useMap } from 'react-leaflet'
import { divIcon, latLngBounds, type CircleMarker as LeafletDaire, type LatLngTuple } from 'leaflet'
import type { DurakliRota, Konum, KoridorYeri, Rota, RotaYaniti, YerTuru } from '../api/tipler'
import type { SeciliDurak, SiraliNokta } from '../rota/duraklar'
import { kategoriAdi } from '../api/kategoriler'
import { leafletKoordinatlari } from './koordinat'

const renkler: Record<YerTuru, string> = { gezi: '#d55b38', mola: '#2b8a73', destek: '#3d71b0' }

function HaritaOdak({ rota, kalkis, varis, seciliYer, durakliRota, siraliDuraklar }: {
  rota: RotaYaniti | null; kalkis: Konum | null; varis: Konum | null; seciliYer: KoridorYeri | null;
  durakliRota: DurakliRota | null; siraliDuraklar: SiraliNokta[]
}) {
  const harita = useMap()
  useEffect(() => {
    if (!rota || !kalkis || !varis) return
    const noktalar: LatLngTuple[] = [...rota.rotalar.flatMap((secenek) => leafletKoordinatlari(secenek.geometri)),
      ...leafletKoordinatlari(durakliRota?.geometri ?? []),
      ...siraliDuraklar.map((nokta): LatLngTuple => [nokta.enlem, nokta.boylam]),
      [kalkis.enlem, kalkis.boylam], [varis.enlem, varis.boylam]]
    harita.fitBounds(latLngBounds(noktalar), { padding: [34, 34] })
  }, [harita, rota, kalkis, varis, durakliRota, siraliDuraklar])
  useEffect(() => {
    if (seciliYer) harita.flyTo([seciliYer.enlem, seciliYer.boylam], Math.max(harita.getZoom(), 12))
  }, [harita, seciliYer])
  return null
}

function YerIsareti({ yer, secili, durakMi, durakDegistir, detayiAc, eklenebilir }: { yer: KoridorYeri; secili: boolean; durakMi: boolean; durakDegistir: (yer: KoridorYeri) => void; detayiAc: (yer: KoridorYeri) => void; eklenebilir: boolean }) {
  const isaret = useRef<LeafletDaire>(null)
  useEffect(() => { if (secili) isaret.current?.openPopup() }, [secili])
  return <CircleMarker ref={isaret} center={[yer.enlem, yer.boylam]} radius={secili ? 9 : 7}
    pathOptions={{ color: '#fff', weight: 2, fillColor: renkler[yer.tur], fillOpacity: 1 }}>
    <Popup><strong>{yer.ad}</strong><br />{kategoriAdi(yer.kategori)}<br />
      <a href="#yer-detayi" onClick={(olay) => { olay.preventDefault(); detayiAc(yer) }}>Detay</a><br />
      <button type="button" disabled={!eklenebilir} onClick={() => durakDegistir(yer)}>{durakMi ? 'Duraktan çıkar' : '+ Durak ekle'}</button></Popup>
  </CircleMarker>
}

export function RotaHaritasi({ rota, seciliRota, rotaSec, kalkis, varis, seciliYer, detayiAc, durakliRota, siraliDuraklar, duraklar, durakDegistir, durakEklenebilir }: {
  rota: RotaYaniti | null; seciliRota: Rota | null; rotaSec: (sira: number) => void;
  kalkis: Konum | null; varis: Konum | null; seciliYer: KoridorYeri | null
  durakliRota: DurakliRota | null; siraliDuraklar: SiraliNokta[]; duraklar: SeciliDurak[]; durakDegistir: (yer: KoridorYeri) => void
  durakEklenebilir: (yer: KoridorYeri) => boolean
  detayiAc: (yer: KoridorYeri) => void
}) {
  return <MapContainer center={[39, 35]} zoom={6} scrollWheelZoom className="harita" aria-label="Rota haritası">
    <div className="harita-lejandi" aria-label="Harita açıklaması">
      {(['gezi', 'mola', 'destek'] as YerTuru[]).map((tur) => <span key={tur}>
        <i style={{ backgroundColor: renkler[tur] }} />{tur[0].toLocaleUpperCase('tr-TR') + tur.slice(1)}
      </span>)}
    </div>
    <TileLayer url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
      attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> katkıda bulunanlar' />
    <HaritaOdak rota={rota} kalkis={kalkis} varis={varis} seciliYer={seciliYer} durakliRota={durakliRota} siraliDuraklar={siraliDuraklar} />
    {rota && <>
      {rota.rotalar.filter((secenek) => secenek.sira !== seciliRota?.sira).map((secenek) =>
        <Polyline key={secenek.sira} positions={leafletKoordinatlari(secenek.geometri)}
          pathOptions={{ color: '#8d969c', weight: 4, opacity: 0.85 }}
          eventHandlers={{ click: () => rotaSec(secenek.sira) }} />)}
      {seciliRota && <Polyline key={`${seciliRota.sira}-${durakliRota ? 'durakli' : 'duz'}`} positions={leafletKoordinatlari(durakliRota?.geometri ?? seciliRota.geometri)}
        pathOptions={{ color: '#204f6b', weight: 7, opacity: 1 }} />}
      {siraliDuraklar.map((nokta, sira) => <Marker key={nokta.durak!.yer.id} position={[nokta.enlem, nokta.boylam]}
        icon={divIcon({ className: 'numarali-durak', html: `<span>${sira + 1}</span>`, iconSize: [28, 28], iconAnchor: [14, 14] })}>
        <Popup>{sira + 1}. {nokta.ad}</Popup>
      </Marker>)}
      {kalkis && <CircleMarker center={[kalkis.enlem, kalkis.boylam]} radius={9}
        pathOptions={{ color: '#fff', weight: 2, fillColor: '#204f6b', fillOpacity: 1 }}><Popup>Kalkış: {kalkis.ad}</Popup></CircleMarker>}
      {varis && <CircleMarker center={[varis.enlem, varis.boylam]} radius={9}
        pathOptions={{ color: '#fff', weight: 2, fillColor: '#242c38', fillOpacity: 1 }}><Popup>Varış: {varis.ad}</Popup></CircleMarker>}
      {(['gezi', 'mola', 'destek'] as YerTuru[]).flatMap((tur) => (seciliRota?.yerler[tur] ?? []).map((yer) =>
        <YerIsareti key={`${tur}-${yer.id}`} yer={yer} secili={seciliYer?.id === yer.id && seciliYer?.tur === tur}
          durakMi={duraklar.some((durak) => durak.yer.id === yer.id)} eklenebilir={durakEklenebilir(yer)} durakDegistir={durakDegistir} detayiAc={detayiAc} />))}
    </>}
  </MapContainer>
}
