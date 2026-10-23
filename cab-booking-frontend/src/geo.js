export async function reverseGeocode(lat, lng) {
  try {
    const res = await fetch(
      `https://nominatim.openstreetmap.org/reverse?format=jsonv2&lat=${lat}&lon=${lng}`
    )
    const data = await res.json()
    if (data.display_name) return data.display_name.split(',').slice(0, 3).join(',')
  } catch {
    // fall through to coordinates
  }
  return `${lat.toFixed(5)}, ${lng.toFixed(5)}`
}