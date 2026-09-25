package com.devott.vendedores;

import com.devott.compartido.errores.ConflictoException;
import com.devott.compartido.errores.DatosInvalidosException;
import com.devott.compartido.errores.RecursoNoEncontradoException;
import com.devott.compartido.texto.Slugs;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class VendedorService {

    private static final GeometryFactory WGS84 = new GeometryFactory(new PrecisionModel(), 4326);

    private final VendedorRepository vendedores;

    VendedorService(VendedorRepository vendedores) {
        this.vendedores = vendedores;
    }

    public Optional<Vendedor> deUsuario(UUID usuarioId) {
        return vendedores.findByUsuarioId(usuarioId);
    }

    public Vendedor porSlug(String slug) {
        return vendedores.findBySlug(slug)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe ese vendedor."));
    }

    @Transactional
    public Vendedor crear(UUID usuarioId, VendedorRequest request) {
        if (vendedores.existsByUsuarioId(usuarioId)) {
            throw new ConflictoException("Ya tenés un perfil de vendedor.");
        }
        Vendedor vendedor = new Vendedor(usuarioId);
        validarSlug(request.slug(), null);
        vendedor.actualizar(datosDe(request));
        return vendedores.save(vendedor);
    }

    @Transactional
    public Vendedor actualizar(UUID usuarioId, VendedorRequest request) {
        Vendedor vendedor = vendedores.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Todavía no creaste tu perfil de vendedor."));
        validarSlug(request.slug(), vendedor.getId());
        vendedor.actualizar(datosDe(request));
        return vendedor;
    }

    /**
     * Motivo por el que el slug no se puede usar, o vacío si está disponible. `usuarioId` permite que
     * el propio vendedor vea su slug actual como disponible.
     */
    public Optional<String> motivoSlugNoDisponible(String slug, UUID usuarioId) {
        UUID vendedorId = vendedores.findByUsuarioId(usuarioId).map(Vendedor::getId).orElse(null);
        try {
            validarSlug(slug, vendedorId);
            return Optional.empty();
        } catch (DatosInvalidosException | ConflictoException e) {
            return Optional.of(e.getMessage());
        }
    }

    private void validarSlug(String slug, UUID vendedorId) {
        if (!Slugs.esValido(slug) || slug.length() < 3 || slug.length() > 40) {
            throw new DatosInvalidosException("slug", "Usá entre 3 y 40 caracteres: minúsculas, números y guiones.");
        }
        if (SlugsReservados.esReservado(slug)) {
            throw new DatosInvalidosException("slug", "Ese link está reservado. Elegí otro.");
        }
        vendedores.findBySlug(slug)
                .filter(otro -> !otro.getId().equals(vendedorId))
                .ifPresent(otro -> {
                    throw new ConflictoException("slug", "Ese link ya está en uso. Elegí otro.");
                });
    }

    private static DatosVendedor datosDe(VendedorRequest r) {
        String whatsapp = Whatsapp.normalizar(r.whatsapp()).orElseThrow(() -> new DatosInvalidosException(
                "whatsapp", "Ingresá el código de área y el número, sin 0 ni 15. Por ejemplo: 3534123456."));
        String instagram;
        String facebook;
        try {
            instagram = RedesSociales.instagram(r.instagram()).orElse(null);
        } catch (IllegalArgumentException e) {
            throw new DatosInvalidosException("instagram", "Ingresá solo tu usuario de Instagram, por ejemplo: @automotores.");
        }
        try {
            facebook = RedesSociales.facebook(r.facebook()).orElse(null);
        } catch (IllegalArgumentException e) {
            throw new DatosInvalidosException("facebook", "Ingresá tu usuario o el link de tu página de Facebook.");
        }
        var localidad = r.localidad();
        return new DatosVendedor(
                r.tipo(),
                r.slug(),
                r.nombrePublico().trim(),
                whatsapp,
                textoOpcional(r.telefono()),
                textoOpcional(r.descripcion()),
                textoOpcional(r.direccion()),
                localidad.ciudad().trim(),
                localidad.provincia().trim(),
                WGS84.createPoint(new Coordinate(localidad.lng(), localidad.lat())),
                textoOpcional(r.horarios()),
                instagram,
                facebook);
    }

    private static String textoOpcional(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}
