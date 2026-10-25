package com.americana.bibliotecaserver.service;

import com.americana.bibliotecaserver.dto.ResumenDashboardRespuesta;
import com.americana.bibliotecaserver.entity.EstadoMulta;
import com.americana.bibliotecaserver.entity.EstadoPrestamo;
import com.americana.bibliotecaserver.entity.EstadoReserva;
import com.americana.bibliotecaserver.entity.EstadoUsuario;
import com.americana.bibliotecaserver.repository.LibroRepository;
import com.americana.bibliotecaserver.repository.MultaRepository;
import com.americana.bibliotecaserver.repository.PrestamoRepository;
import com.americana.bibliotecaserver.repository.ReservaRepository;
import com.americana.bibliotecaserver.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {

    private final LibroRepository libroRepository;
    private final PrestamoRepository prestamoRepository;
    private final ReservaRepository reservaRepository;
    private final UsuarioRepository usuarioRepository;
    private final MultaRepository multaRepository;

    public DashboardService(LibroRepository libroRepository,
            PrestamoRepository prestamoRepository, ReservaRepository reservaRepository,
            UsuarioRepository usuarioRepository, MultaRepository multaRepository) {
        this.libroRepository = libroRepository;
        this.prestamoRepository = prestamoRepository;
        this.reservaRepository = reservaRepository;
        this.usuarioRepository = usuarioRepository;
        this.multaRepository = multaRepository;
    }

    @Transactional(readOnly = true)
    public ResumenDashboardRespuesta consultar() {
        return new ResumenDashboardRespuesta(
                libroRepository.count(),
                libroRepository.sumarCantidadDisponible(),
                prestamoRepository.countByEstado(EstadoPrestamo.ACTIVO),
                prestamoRepository.countByEstado(EstadoPrestamo.ATRASADO),
                reservaRepository.countByEstado(EstadoReserva.PENDIENTE),
                usuarioRepository.countByEstado(EstadoUsuario.ACTIVO),
                multaRepository.sumarMontoPorEstado(EstadoMulta.PENDIENTE));
    }
}
