const COLORS = {
  REQUESTED: 'bg-yellow-100 text-yellow-800',
  ACCEPTED: 'bg-blue-100 text-blue-800',
  STARTED: 'bg-green-100 text-green-800',
  COMPLETED: 'bg-gray-200 text-gray-800',
  CANCELLED: 'bg-red-100 text-red-700',
}

export default function StatusBadge({ status }) {
  return (
    <span className={`px-2 py-1 rounded text-xs font-semibold ${COLORS[status] || 'bg-gray-100'}`}>
      {status}
    </span>
  )
}