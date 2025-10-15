export const getRelativeTime = (dateString) => {
  if (!dateString) return ''
  const rtf = new Intl.RelativeTimeFormat('es-AR', { numeric: 'auto' })
  const now = new Date()
  const date = new Date(dateString)
  const diffMs = date.getTime() - now.getTime()
  const minutes = Math.round(diffMs / 60000)
  const hours = Math.round(minutes / 60)
  const days = Math.round(hours / 24)
  if (Math.abs(minutes) < 60) return rtf.format(minutes, 'minute')
  if (Math.abs(hours) < 24) return rtf.format(hours, 'hour')
  return rtf.format(days, 'day')
}

export const formatCurrency = (value, currency) => {
  try {
    const formatted = new Intl.NumberFormat('es-AR', {
      style: 'currency',
      currency: currency || 'ARS',
      minimumFractionDigits: 2
    }).format(Number(value))
    return formatted.replace(/^BRL\s*/, 'R$ ')
  } catch (e) {
    return `$${Number(value || 0).toFixed(2)}`
  }
}

export const translateStatus = (status) => {
  const statusMap = {
    'AUTHORIZED': 'Autorizada',
    'PAID': 'Pagado',
    'REFUNDED': 'Reembolsado',
    'FAILED': 'Falló'
  }
  return statusMap[status] || status
}


