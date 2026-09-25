package com.devott.publicaciones;

import com.devott.catalogo.ModeloConMarca;

import java.util.List;

/**
 * Publicación con los datos que hacen falta para mostrarla: su modelo y marca y sus fotos en orden.
 */
record PublicacionVista(Publicacion publicacion, ModeloConMarca modelo, List<Foto> fotos) {
}
