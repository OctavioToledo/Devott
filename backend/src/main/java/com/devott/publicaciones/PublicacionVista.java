package com.devott.publicaciones;

import com.devott.catalogo.ModeloConMarca;

/**
 * Publicación con los datos que hacen falta para mostrarla: su modelo y marca y la cantidad de fotos.
 */
record PublicacionVista(Publicacion publicacion, ModeloConMarca modelo, long cantidadFotos) {
}
