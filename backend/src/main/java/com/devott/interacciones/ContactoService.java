package com.devott.interacciones;

import com.devott.usuarios.UsuarioService;
import com.devott.vendedores.Vendedor;
import com.devott.vendedores.VendedorService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@Service
public class ContactoService {

    private final VendedorService vendedores;
    private final UsuarioService usuarios;
    private final ContactoRepository contactos;
    private final String urlDelSitio;

    ContactoService(VendedorService vendedores, UsuarioService usuarios, ContactoRepository contactos,
                    @Value("${devott.frontend-url}") String urlDelSitio) {
        this.vendedores = vendedores;
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

    static URI linkDeWhatsapp(String numero, String mensaje) {
        return UriComponentsBuilder.fromUriString("https://wa.me/" + numero)
                .queryParam("text", mensaje)
                .encode()
                .build()
                .toUri();
    }
}
