# 🎈 ChatGlobo

**ChatGlobo** es un plugin moderno y ligero para servidores **PaperMC (1.21)** que muestra burbujas de texto flotantes sobre las cabezas de los jugadores cuando escriben en el chat.

Diseñado para mejorar la interacción social y el roleplay, utilizando las nuevas **Display Entities** de Minecraft para un rendimiento máximo sin lag.

## ✨ Características Principales

* **🗣️ Automático e Inmersivo:** No necesitas comandos para hablar. Simplemente escribe en el chat y aparecerá el globo.
* **🎨 Soporte de Colores:** Compatible con códigos de color clásicos (`&a`, `&c`, `&l`, etc.) y formato de chat.
* **🚀 Rendimiento Optimizado:** Usa *Text Display Entities* (nativo de 1.21), lo que significa cero lag y movimientos suaves pegados al jugador.
* **📏 Altura Ajustable en Vivo:** ¿El globo está muy alto o muy bajo? ¡Cámbialo con un comando sin reiniciar!
* **💾 Persistencia de Datos:** El plugin recuerda tus configuraciones (quién ocultó el globo, la altura definida) incluso después de reiniciar el servidor.
* **🛡️ Control Total:** Comandos para administradores (apagado global) y para usuarios (apagado personal).

---

## 🎮 Comandos y Permisos

### Para Jugadores
| Comando | Descripción | Permiso |
| :--- | :--- | :--- |
| `/globo` | Activa o desactiva tus propios globos de texto. Útil si quieres ser discreto. | Ninguno |

### Para Administradores
| Comando | Descripción | Permiso |
| :--- | :--- | :--- |
| `/globoglobal` | Activa o desactiva el plugin para **todos** en el servidor. | `chatglobo.admin` |
| `/globoaltura <n>` | Define la altura del globo (ej. `0.625`). Se guarda automáticamente. | `chatglobo.admin` |

---

## ⚙️ Configuración (`config.yml`)

El archivo `config.yml` se genera automáticamente. Aquí se guardan tus preferencias:

```yaml
# Interruptor general del plugin (true = activado, false = desactivado)
global-activo: true

# Altura del globo sobre la cabeza del jugador (en bloques)
# 0.625 es ideal para estar pegado a la cabeza sin tocarla
altura-globo: 0.625

# Lista de jugadores que tienen el globo desactivado personalmente
# (No editar manualmente)
usuarios-ocultos: []
```

---
Creado por **Masitas**.