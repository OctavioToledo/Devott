package com.devott.interacciones;

import com.devott.publicaciones.Moneda;
import com.devott.publicaciones.PublicacionService;
import com.devott.publicaciones.PublicacionService.ParaContacto;
import com.devott.usuarios.UsuarioService;
import com.devott.vendedores.Vendedor;
import com.devott.vendedores.VendedorService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.net.URI;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.UUID;

@Service
public class ContactoService {

    private final VendedorService vendedores;
    private final PublicacionService publicaciones;
    private final UsuarioService usuarios;
    private final ContactoRepository contactos;
    private final String urlDelSitio;

    ContactoService(VendedorService vendedores, PublicacionService publicaciones, UsuarioService usuarios,
                    ContactoRepository contactos, @Value("${devott.frontend-url}") String urlDelSitio) {
        this.vendedores = vendedores;
        this.publicaciones = publicaciones;
        this.usuarios = usuarios;
        this.contactos = contactos;
        this.urlDelSitio = urlDelSitio;
    }

    /**
     * Registra el contacto desde el perfil del vendedor y devuelve el link de WhatsApp con un mensaje
     * precargado. `jwt` es null si el comprador no inició sesión.
     */
    @Transactional
    public URI contactarVendedor(String slug, Jwt jwt) {
        Vendedor vendedor = vendedores.porSlug(slug);
        UUID usuarioId = jwt == null ? null : usuarios.sincronizar(jwt).getId();
        contactos.save(new Contacto(null, vendedor.getId(), usuarioId));

        String mensaje = "¡Hola! Vi tu perfil en Devott (" + urlDelSitio + "/" + vendedor.getSlug()
                + ") y quería hacerte una consulta.";
        return linkDeWhatsapp(vendedor.getWhatsapp(), mensaje);
    }

    /**
     * Registra el contacto por una publicación activa y devuelve el link de WhatsApp con un mensaje
     * que dice qué auto es, su precio y el link. `jwt` es null si el comprador no inició sesión.
     */
    @Transactional
    public URI contactarPorPublicacion(String slug, Jwt jwt) {
        ParaContacto p = publicaciones.paraContacto(slug);
        Vendedor vendedor = vendedores.porId(p.vendedorId()).orElseThrow();
        UUID usuarioId = jwt == null ? null : usuarios.sincronizar(jwt).getId();
        contactos.save(new Contacto(p.publicacionId(), vendedor.getId(), usuarioId));
        return linkDeWhatsapp(vendedor.getWhatsapp(), mensajePorPublicacion(p, urlDelSitio));
    }

    /**
     * "¡Hola! Vi tu Toyota Hilux 2.8 SRV 4x4 AT 2021 publicado en Devott a US$ 34.900 y quería
     * consultarte si sigue disponible." y el link en la línea siguiente.
     */
    static String mensajePorPublicacion(ParaContacto p, String urlDelSitio) {
        String auto = p.titulo() + (p.version() == null ? "" : " " + p.version()) + " " + p.anio();
        return "¡Hola! Vi tu " + auto + " publicado en Devott a " + precio(p.precio(), p.moneda())
                + " y quería consultarte si sigue disponible.\n" + urlDelSitio + "/publicaciones/" + p.slug();
    }

    /** "US$ 34.900" o "$ 25.900.000", igual que en el sitio. */
    static String precio(BigDecimal precio, Moneda moneda) {
        NumberFormat miles = NumberFormat.getIntegerInstance(Locale.forLanguageTag("es-AR"));
        return (moneda == Moneda.USD ? "US$ " : "$ ") + miles.format(precio);
    }

    static URI linkDeWhatsapp(String numero, String mensaje) {
        return UriComponentsBuilder.fromUriString("https://wa.me/" + numero)
                .queryParam("text", mensaje)
                .encode()
                .build()
                .toUri();
    }
}
