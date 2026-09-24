import { useEffect, useRef } from 'react'
import { CircleMarker, MapContainer, Popup, Polyline, TileLayer, useMap } from 'react-leaflet'
import { latLngBounds, type CircleMarker as LeafletDaire, type LatLngTuple } from 'leaflet'
import type { Konum, KoridorYeri, Rota, RotaYaniti, YerTuru } from '../api/tipler'
import { kategoriAdi } from '../api/kategoriler'
import { leafletKoordinatlari } from './koordinat'

const renkler: Record<YerTuru, string> = { gezi: '#d55b38', mola: '#2b8a73', destek: '#3d71b0' }

function HaritaOdak({ rota, kalkis, varis, seciliYer }: {
  rota: RotaYaniti | null; kalkis: Konum | null; varis: Konum | null; seciliYer: KoridorYeri | null
}) {
  const harita = useMap()
  useEffect(() => {
    if (!rota || !kalkis || !varis) return
    const noktalar: LatLngTuple[] = [...rota.rotalar.flatMap((secenek) => leafletKoordinatlari(secenek.geometri)),
      [kalkis.enlem, kalkis.boylam], [varis.enlem, varis.boylam]]
    harita.fitBounds(latLngBounds(noktalar), { padding: [34, 34] })
  }, [harita, rota, kalkis, varis])
  useEffect(() => {
    if (seciliYer) harita.flyTo([seciliYer.enlem, seciliYer.boylam], Math.max(harita.getZoom(), 12))
  }, [harita, seciliYer])
  return null
}

function YerIsareti({ yer, secili }: { yer: KoridorYeri; secili: boolean }) {
  const isaret = useRef<LeafletDaire>(null)
  useEffect(() => { if (secili) isaret.current?.openPopup() }, [secili])
  return <CircleMarker ref={isaret} center={[yer.enlem, yer.boylam]} radius={secili ? 9 : 7}
    pathOptions={{ color: '#fff', weight: 2, fillColor: renkler[yer.tur], fillOpacity: 1 }}>
    <Popup><strong>{yer.ad}</strong><br />{kategoriAdi(yer.kategori)}</Popup>
  </CircleMarker>
}

export function RotaHaritasi({ rota, seciliRota, rotaSec, kalkis, varis, seciliYer }: {
  rota: RotaYaniti | null; seciliRota: Rota | null; rotaSec: (sira: number) => void;
  kalkis: Konum | null; varis: Konum | null; seciliYer: KoridorYeri | null
}) {
  return <MapContainer center={[39, 35]} zoom={6} scrollWheelZoom className="harita" aria-label="Rota haritası">
    <div className="harita-lejandi" aria-label="Harita açıklaması">
      {(['gezi', 'mola', 'destek'] as YerTuru[]).map((tur) => <span key={tur}>
        <i style={{ backgroundColor: renkler[tur] }} />{tur[0].toLocaleUpperCase('tr-TR') + tur.slice(1)}
      </span>)}
    </div>
    <TileLayer url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
      attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> katkıda bulunanlar' />
    <HaritaOdak rota={rota} kalkis={kalkis} varis={varis} seciliYer={seciliYer} />
    {rota && <>
      {rota.rotalar.filter((secenek) => secenek.sira !== seciliRota?.sira).map((secenek) =>
        <Polyline key={secenek.sira} positions={leafletKoordinatlari(secenek.geometri)}
          pathOptions={{ color: '#8d969c', weight: 4, opacity: 0.85 }}
          eventHandlers={{ click: () => rotaSec(secenek.sira) }} />)}
      {seciliRota && <Polyline key={seciliRota.sira} positions={leafletKoordinatlari(seciliRota.geometri)}
        pathOptions={{ color: '#204f6b', weight: 7, opacity: 1 }} />}
      {kalkis && <CircleMarker center={[kalkis.enlem, kalkis.boylam]} radius={9}
        pathOptions={{ color: '#fff', weight: 2, fillColor: '#204f6b', fillOpacity: 1 }}><Popup>Kalkış: {kalkis.ad}</Popup></CircleMarker>}
      {varis && <CircleMarker center={[varis.enlem, varis.boylam]} radius={9}
        pathOptions={{ color: '#fff', weight: 2, fillColor: '#242c38', fillOpacity: 1 }}><Popup>Varış: {varis.ad}</Popup></CircleMarker>}
      {(['gezi', 'mola', 'destek'] as YerTuru[]).flatMap((tur) => (seciliRota?.yerler[tur] ?? []).map((yer) =>
        <YerIsareti key={`${tur}-${yer.id}`} yer={yer} secili={seciliYer?.id === yer.id && seciliYer?.tur === tur} />))}
    </>}
  </MapContainer>
}
