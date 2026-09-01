package io.heimui.core

import io.heimui.core.data.mapper.toDomain
import io.heimui.core.data.serialization.HeimJson
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Every example under `schema/` must parse and map with the real SDK pipeline.
 *
 * This is what keeps the published schema honest. A schema that documents a shape the client
 * cannot actually render is worse than no schema: a backend team would build against it, pass
 * their own CI, and still ship a broken screen.
 */
class HeimSchemaExamplesTest {

    private val examples: Map<String, String> = mapOf(
        "01-minimal" to """{
  "id": "minimal",
  "root": {
    "type": "container",
    "id": "root",
    "children": [
      {
        "type": "text",
        "id": "hello",
        "text": "Hola HeimUI"
      }
    ]
  }
}
""",
        "02-container-y-box" to """{
  "id": "layout",
  "title": "Layout",
  "root": {
    "type": "container",
    "id": "root",
    "direction": "VERTICAL",
    "padding": 16,
    "spacing": 12,
    "background_color": "surface",
    "children": [
      {
        "type": "box",
        "id": "overlay",
        "content_alignment": "CENTER",
        "children": [
          {
            "type": "image",
            "id": "bg",
            "url": "https://picsum.photos/800/400",
            "aspect_ratio": 2.0,
            "corner_radius": 12
          },
          {
            "type": "badge",
            "id": "tag",
            "text": "NUEVO",
            "background_color": "primaryContainer"
          }
        ]
      },
      {
        "type": "divider",
        "id": "sep",
        "thickness": 1
      },
      {
        "type": "spacer",
        "id": "gap",
        "size": 24
      }
    ]
  }
}
""",
        "03-formulario" to """{
  "id": "registro",
  "title": "Registro",
  "root": {
    "type": "container",
    "id": "root",
    "padding": 16,
    "spacing": 16,
    "children": [
      {
        "type": "text",
        "id": "h",
        "text": "Crear cuenta",
        "style": "headlineSmall",
        "a11y": {
          "content_description": "Crear cuenta",
          "is_heading": true
        }
      },
      {
        "type": "text_field",
        "id": "f_nombre",
        "state_key": "nombre",
        "label": "Nombre completo",
        "validation_rules": [
          {
            "type": "REQUIRED",
            "error_message": "El nombre es obligatorio"
          }
        ]
      },
      {
        "type": "text_field",
        "id": "f_email",
        "state_key": "email",
        "label": "Email",
        "input_type": "EMAIL",
        "helper_text": "Te enviaremos un código",
        "validation_rules": [
          {
            "type": "REQUIRED",
            "error_message": "El email es obligatorio"
          },
          {
            "type": "EMAIL",
            "error_message": "Email inválido"
          }
        ]
      },
      {
        "type": "text_field",
        "id": "f_pass",
        "state_key": "password",
        "label": "Contraseña",
        "input_type": "PASSWORD",
        "validation_rules": [
          {
            "type": "MIN_LENGTH",
            "value": "8",
            "error_message": "Mínimo 8 caracteres"
          }
        ]
      },
      {
        "type": "switch",
        "id": "sw_empresa",
        "state_key": "es_empresa",
        "label": "¿Cuenta empresarial?"
      },
      {
        "type": "text_field",
        "id": "f_nit",
        "state_key": "nit",
        "label": "NIT",
        "visible_if": "es_empresa",
        "validation_rules": [
          {
            "type": "CUSTOM",
            "value": "COLOMBIAN_NIT",
            "error_message": "NIT inválido"
          }
        ]
      },
      {
        "type": "button",
        "id": "btn_enviar",
        "title": "Crear cuenta",
        "is_full_width": true,
        "actions": [
          {
            "type": "submit_form",
            "endpoint": "/ms-cuentas/registro",
            "method": "POST",
            "payload": {
              "nombre": "{{state.nombre}}",
              "email": "{{state.email}}",
              "empresarial": "{{state.es_empresa}}",
              "origen": "mobile"
            }
          }
        ]
      }
    ]
  }
}
""",
        "04-lista-paginada" to """{
  "id": "feed",
  "title": "Feed",
  "root": {
    "type": "lazy_column",
    "id": "lista",
    "spacing": 12,
    "padding": 16,
    "pagination": {
      "next_cursor": "cursor_p2",
      "has_more": true,
      "load_threshold": 3,
      "on_load_more_actions": [
        {
          "type": "custom",
          "name": "load_more",
          "payload": {
            "cursor": "cursor_p2"
          }
        }
      ]
    },
    "items": [
      {
        "type": "card",
        "id": "item_1",
        "elevation": 2,
        "padding": 16,
        "actions": [
          {
            "type": "navigate",
            "screen_id": "detalle",
            "params": {
              "id": "1"
            }
          }
        ],
        "child": {
          "type": "container",
          "id": "c_1",
          "spacing": 8,
          "scrollable": false,
          "children": [
            {
              "type": "text",
              "id": "t_1",
              "text": "Producto uno",
              "style": "titleMedium"
            },
            {
              "type": "text",
              "id": "p_1",
              "text": "${'$'}120.000",
              "color": "primary"
            }
          ]
        }
      },
      {
        "type": "card",
        "id": "item_2",
        "elevation": 2,
        "padding": 16,
        "child": {
          "type": "text",
          "id": "t_2",
          "text": "Producto dos",
          "max_lines": 2
        }
      }
    ]
  }
}
""",
        "05-modales-y-acciones" to """{
  "id": "acciones",
  "root": {
    "type": "container",
    "id": "root",
    "padding": 16,
    "spacing": 12,
    "children": [
      {
        "type": "button",
        "id": "b_sheet",
        "title": "Abrir hoja",
        "variant": "OUTLINED",
        "actions": [
          {
            "type": "show_bottom_sheet",
            "title": "Detalles",
            "is_dismissible": true,
            "content": {
              "type": "container",
              "id": "sheet",
              "padding": 16,
              "scrollable": false,
              "children": [
                {
                  "type": "text",
                  "id": "s_t",
                  "text": "Contenido de la hoja"
                },
                {
                  "type": "button",
                  "id": "s_c",
                  "title": "Cerrar",
                  "actions": [
                    {
                      "type": "dismiss_modal"
                    }
                  ]
                }
              ]
            }
          }
        ]
      },
      {
        "type": "button",
        "id": "b_dialog",
        "title": "Confirmar",
        "variant": "TONAL",
        "actions": [
          {
            "type": "show_dialog",
            "title": "¿Seguro?",
            "message": "Esta acción no se puede deshacer.",
            "confirm_text": "Sí",
            "dismiss_text": "No",
            "confirm_actions": [
              {
                "type": "show_snackbar",
                "message": "Hecho",
                "duration": "SHORT"
              }
            ]
          }
        ]
      },
      {
        "type": "button",
        "id": "b_url",
        "title": "Términos",
        "variant": "TEXT",
        "actions": [
          {
            "type": "open_url",
            "url": "https://heimui.io/terminos"
          }
        ]
      },
      {
        "type": "icon",
        "id": "ic",
        "name": "info",
        "tint": "onSurfaceVariant",
        "size": 24
      }
    ]
  }
}
""",
        "06-desconocido-y-custom" to """{
  "id": "futuro",
  "root": {
    "type": "container",
    "id": "root",
    "children": [
      {
        "type": "text",
        "id": "antes",
        "text": "Antes del desconocido"
      },
      {
        "type": "carousel_3d",
        "id": "futuro_1"
      },
      {
        "type": "custom",
        "id": "grafico",
        "name": "stock_chart",
        "data": {
          "ticker": "AAPL",
          "volumen": 15000
        }
      },
      {
        "type": "text",
        "id": "despues",
        "text": "Después: el árbol sigue vivo"
      }
    ]
  }
}
""",
    )

    @Test
    fun everyPublishedExampleParsesAndMaps() {
        assertTrue(examples.isNotEmpty())
        examples.forEach { (name, raw) ->
            val screen = HeimJson.decodeScreen(raw).toDomain()
            assertTrue(screen.id.isNotBlank(), "$name produced a blank screen id")
            assertTrue(screen.root.id.isNotBlank(), "$name produced a blank root")
        }
    }
}
