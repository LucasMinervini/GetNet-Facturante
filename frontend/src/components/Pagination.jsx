import React from 'react'

export default function Pagination({ currentPage, totalPages, onFirst, onPrev, onNext, onLast }) {
  if (totalPages <= 1) return null
  return (
    <div className="pagination">
      <button onClick={onFirst} disabled={currentPage === 0} className="pagination-btn">
        Primera
      </button>
      <button onClick={onPrev} disabled={currentPage === 0} className="pagination-btn">
        Anterior
      </button>
      <span className="pagination-info">
        Página {currentPage + 1} de {totalPages}
      </span>
      <button onClick={onNext} disabled={currentPage >= totalPages - 1} className="pagination-btn">
        Siguiente
      </button>
      <button onClick={onLast} disabled={currentPage >= totalPages - 1} className="pagination-btn">
        Última
      </button>
    </div>
  )
}


