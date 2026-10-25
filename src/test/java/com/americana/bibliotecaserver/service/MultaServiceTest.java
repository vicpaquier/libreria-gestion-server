package com.americana.bibliotecaserver.service;

import com.americana.bibliotecaserver.dto.CrearMultaSolicitud;
import com.americana.bibliotecaserver.entity.EstadoMulta;
import com.americana.bibliotecaserver.entity.Multa;
import com.americana.bibliotecaserver.entity.Prestamo;
import com.americana.bibliotecaserver.exception.ConflictoNegocioException;
import com.americana.bibliotecaserver.mapper.MultaMapper;
import com.americana.bibliotecaserver.repository.MultaRepository;
import com.americana.bibliotecaserver.repository.PrestamoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MultaServiceTest {

    private MultaRepository multaRepository;
    private PrestamoRepository prestamoRepository;
    private MultaService multaService;

    @BeforeEach
    void configurar() {
        multaRepository = mock(MultaRepository.class);
        prestamoRepository = mock(PrestamoRepository.class);
        multaService = new MultaService(multaRepository, prestamoRepository,
                mock(MultaMapper.class), Clock.fixed(
                        Instant.parse("2026-09-08T15:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void registraManualmenteComoPendiente() {
        when(prestamoRepository.findById(3L)).thenReturn(Optional.of(mock(Prestamo.class)));
        when(multaRepository.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));

        multaService.crear(new CrearMultaSolicitud(3L, new BigDecimal("50000"), " Atraso "));

        var captor = org.mockito.ArgumentCaptor.forClass(Multa.class);
        verify(multaRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getEstado()).isEqualTo(EstadoMulta.PENDIENTE);
        assertThat(captor.getValue().getMotivo()).isEqualTo("Atraso");
    }

    @Test
    void noPermitePagarUnaMultaFinalizada() {
        var multa = mock(Multa.class);
        when(multa.getEstado()).thenReturn(EstadoMulta.CANCELADA);
        when(multaRepository.buscarPorIdConBloqueo(5L)).thenReturn(Optional.of(multa));

        assertThatThrownBy(() -> multaService.pagar(5L))
                .isInstanceOf(ConflictoNegocioException.class);
        verify(multa, never()).pagar();
    }
}
