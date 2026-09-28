package com.devott.cotizaciones;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class CotizacionService {

    /** El mercado de autos usados cotiza en dólar blue: con ese se convierten los precios en pesos. */
    public static final TipoCotizacion REFERENCIA = TipoCotizacion.BLUE;

    private final ProveedorDeCotizaciones proveedor;
    private final JdbcClient jdbc;

    CotizacionService(ProveedorDeCotizaciones proveedor, JdbcClient jdbc) {
        this.proveedor = proveedor;
        this.jdbc = jdbc;
    }

    /** Pesos por dólar según la última cotización de referencia guardada, si hay alguna. */
    public Optional<BigDecimal> dolarDeReferencia() {
        return jdbc.sql("SELECT valor FROM cotizacion WHERE tipo = ? ORDER BY fecha DESC LIMIT 1")
                .param(REFERENCIA.name())
                .query(BigDecimal.class)
                .optional();
    }

    /** Trae la cotización de referencia del proveedor y la guarda (reemplaza la del mismo día). */
    @Transactional
    public Cotizacion actualizar() {
        Cotizacion cotizacion = proveedor.actual(REFERENCIA);
        jdbc.sql("""
                        INSERT INTO cotizacion (fecha, tipo, valor) VALUES (?, ?, ?)
                        ON CONFLICT (fecha, tipo) DO UPDATE SET valor = EXCLUDED.valor
                        """)
                .params(cotizacion.fecha(), cotizacion.tipo().name(), cotizacion.valor())
                .update();
        return cotizacion;
    }
}
