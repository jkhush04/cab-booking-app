import { useEffect } from 'react'
import { MapContainer, Marker, TileLayer, Tooltip, useMap, useMapEvents } from 'react-leaflet'
import L from 'leaflet'
import markerIcon from 'leaflet/dist/images/marker-icon.png'
import markerIcon2x from 'leaflet/dist/images/marker-icon-2x.png'
import markerShadow from 'leaflet/dist/images/marker-shadow.png'

delete L.Icon.Default.prototype._getIconUrl
L.Icon.Default.mergeOptions({
  iconRetinaUrl: markerIcon2x,
  iconUrl: markerIcon,
  shadowUrl: markerShadow,
})

function ClickHandler({ onClick }) {
  useMapEvents({ click: (e) => onClick && onClick(e.latlng) })
  return null
}

function FitBounds({ markers }) {
  const map = useMap()
  const key = markers.map((m) => `${m.lat},${m.lng}`).join('|')
  useEffect(() => {
    if (markers.length >= 2) {
      map.fitBounds(markers.map((m) => [m.lat, m.lng]), { padding: [50, 50] })
    } else if (markers.length === 1) {
      map.setView([markers[0].lat, markers[0].lng], Math.max(map.getZoom(), 14))
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [key])
  return null
}

export default function RideMap({ markers = [], onClick, height = 340 }) {
  return (
    <MapContainer
      center={[23.2599, 77.4126]}
      zoom={12}
      style={{ height, width: '100%' }}
      className="rounded-xl z-0"
    >
      <TileLayer
        attribution="&copy; OpenStreetMap contributors"
        url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
      />
      <ClickHandler onClick={onClick} />
      <FitBounds markers={markers} />
      {markers.map((m) => (
        <Marker key={m.label} position={[m.lat, m.lng]}>
          <Tooltip permanent direction="top">{m.label}</Tooltip>
        </Marker>
      ))}
    </MapContainer>
  )
}