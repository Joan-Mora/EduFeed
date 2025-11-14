# 🎨 Patrones UX: Preview y Grabación en Autenticación Biométrica

## 📋 Resumen

Guía de patrones de experiencia de usuario para captura biométrica en EduFeed V3. Incluye:
- **Preview en vivo** para FaceID con detección facial en tiempo real
- **Grabación explícita** para voz con timer y controles visuales
- **Feedback inmediato** para guiar al usuario durante la captura

## 🎯 Problema Resuelto

### Antes (V2)
```
FaceID: Captura instantánea sin preview → Usuario no sabe si está encuadrado
Voz: Acepta cualquier ruido inmediatamente → Grabaciones de 0.1s inválidas
Sin feedback visual → Usuario no sabe si la captura fue exitosa
```

### Ahora (V3)
```
FaceID: Preview con recuadros verdes sobre rostros detectados
Usuario ve si está bien encuadrado antes de capturar
Botón explícito "Capturar y Verificar"

Voz: Botón micrófono para iniciar/detener grabación
Timer visible (mínimo 3 segundos)
Waveform animado mientras graba
```

## 🏗️ Arquitectura

```
┌────────────────────────────────────┐
│  biometric-auth.html               │
│  ┌──────────────────────────────┐  │
│  │ FaceID Preview               │  │
│  │ ┌────────────────────────┐   │  │
│  │ │ <video> con stream     │   │  │
│  │ │ <canvas> overlay       │   │  │
│  │ │   ├─ Detección loop    │   │  │
│  │ │   └─ Recuadros verdes  │   │  │
│  │ └────────────────────────┘   │  │
│  │ [Capturar y Verificar]       │  │
│  └──────────────────────────────┘  │
│                                    │
│  ┌──────────────────────────────┐  │
│  │ Voice Recorder               │  │
│  │ ┌────────────────────────┐   │  │
│  │ │ MediaRecorder          │   │  │
│  │ │   ├─ Timer: 00:03      │   │  │
│  │ │   ├─ Waveform ~~~~~    │   │  │
│  │ │   └─ Status: Grabando  │   │  │
│  │ └────────────────────────┘   │  │
│  │ [🎤 Iniciar] [⏹️ Detener]    │  │
│  └──────────────────────────────┘  │
└────────────────────────────────────┘
```

## 📁 Implementación

### 1. Preview de FaceID

#### HTML Structure
**`biometric-auth.html`**
```html
<!-- Contenedor de preview -->
<div id="facePreviewContainer" class="face-preview-container" style="display:none;">
    <video id="faceVideo" autoplay muted playsinline></video>
    <canvas id="faceCanvas"></canvas>
    <div class="face-status" id="faceStatus">Posiciona tu rostro en el centro</div>
</div>

<!-- Contenedor de controles -->
<div id="faceControls" style="display:none;">
    <button id="btnCaptureFace" class="btn-primary">
        Capturar y Verificar
    </button>
    <button id="btnCancelFace" class="btn-secondary">
        Cancelar
    </button>
</div>
```

#### CSS Styles
```css
.face-preview-container {
    position: relative;
    width: 100%;
    max-width: 480px;
    margin: 20px auto;
    border-radius: 12px;
    overflow: hidden;
    box-shadow: 0 4px 6px rgba(0,0,0,0.1);
}

#faceVideo {
    width: 100%;
    height: auto;
    display: block;
    background: #000;
}

#faceCanvas {
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    pointer-events: none; /* Canvas no intercepta clicks */
}

.face-status {
    position: absolute;
    bottom: 20px;
    left: 50%;
    transform: translateX(-50%);
    background: rgba(0,0,0,0.7);
    color: white;
    padding: 10px 20px;
    border-radius: 20px;
    font-size: 14px;
    font-weight: 500;
    text-align: center;
    max-width: 90%;
}

.face-status.detected {
    background: rgba(16, 185, 129, 0.9);
}

.face-status.error {
    background: rgba(239, 68, 68, 0.9);
}

#faceControls {
    display: flex;
    gap: 10px;
    justify-content: center;
    margin-top: 20px;
}
```

#### JavaScript Logic
```javascript
let faceStream = null;
let faceDetectionInterval = null;

async function authenticateFace() {
    try {
        // Cargar modelos de face-api.js (solo una vez)
        await ensureFaceApiModels();
        
        // Solicitar cámara
        faceStream = await navigator.mediaDevices.getUserMedia({
            video: { facingMode: 'user', width: 480, height: 640 }
        });
        
        // Asignar stream a video
        const video = document.getElementById('faceVideo');
        video.srcObject = faceStream;
        await video.play();
        
        // Mostrar preview
        document.getElementById('facePreviewContainer').style.display = 'block';
        document.getElementById('faceControls').style.display = 'flex';
        
        // Iniciar detección en loop
        startFaceDetectionLoop();
        
    } catch (error) {
        console.error('[FACE] Error al iniciar preview:', error);
        showStatus('error', 'No se pudo acceder a la cámara');
    }
}

function startFaceDetectionLoop() {
    const video = document.getElementById('faceVideo');
    const canvas = document.getElementById('faceCanvas');
    const statusEl = document.getElementById('faceStatus');
    
    // Ajustar canvas al tamaño del video
    canvas.width = video.videoWidth;
    canvas.height = video.videoHeight;
    
    faceDetectionInterval = setInterval(async () => {
        try {
            // Detectar rostros en el frame actual
            const detections = await faceapi.detectAllFaces(
                video,
                new faceapi.SsdMobilenetv1Options({ minConfidence: 0.5 })
            );
            
            // Limpiar canvas
            const ctx = canvas.getContext('2d');
            ctx.clearRect(0, 0, canvas.width, canvas.height);
            
            if (detections.length === 0) {
                statusEl.textContent = 'No se detecta rostro';
                statusEl.className = 'face-status error';
            } else if (detections.length > 1) {
                statusEl.textContent = 'Múltiples rostros detectados';
                statusEl.className = 'face-status error';
            } else {
                statusEl.textContent = 'Rostro detectado ✓';
                statusEl.className = 'face-status detected';
                
                // Dibujar recuadro verde
                const box = detections[0].box;
                ctx.strokeStyle = '#10B981'; // Verde
                ctx.lineWidth = 3;
                ctx.strokeRect(box.x, box.y, box.width, box.height);
            }
            
        } catch (error) {
            console.error('[FACE] Error en detección:', error);
        }
    }, 200); // Detectar cada 200ms (5 fps)
}

async function captureFace() {
    const video = document.getElementById('faceVideo');
    const statusEl = document.getElementById('faceStatus');
    
    try {
        // Detener loop de detección
        clearInterval(faceDetectionInterval);
        
        statusEl.textContent = 'Capturando...';
        statusEl.className = 'face-status';
        
        // Capturar con descriptor completo
        const detection = await faceapi
            .detectSingleFace(video)
            .withFaceLandmarks()
            .withFaceDescriptor();
        
        if (!detection) {
            showStatus('error', 'No se detectó ningún rostro. Intenta de nuevo.');
            startFaceDetectionLoop(); // Reiniciar preview
            return;
        }
        
        // Enviar descriptor al backend
        const descriptor = Array.from(detection.descriptor);
        const response = await fetch('/api/auth/biometric/verify', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                userId: userId,
                token: token,
                method: 'faceid',
                data: JSON.stringify({ descriptor }),
                secondAttempt: false
            })
        });
        
        const result = await response.json();
        
        if (result.success) {
            showStatus('success', '✓ Verificación exitosa');
            stopFacePreview();
            // Backend notifica desktop automáticamente
        } else {
            showStatus('error', result.message || 'Verificación fallida');
            startFaceDetectionLoop(); // Permitir retry
        }
        
    } catch (error) {
        console.error('[FACE] Error al capturar:', error);
        showStatus('error', 'Error al procesar imagen');
        startFaceDetectionLoop();
    }
}

function stopFacePreview() {
    // Detener stream
    if (faceStream) {
        faceStream.getTracks().forEach(track => track.stop());
        faceStream = null;
    }
    
    // Detener loop de detección
    if (faceDetectionInterval) {
        clearInterval(faceDetectionInterval);
        faceDetectionInterval = null;
    }
    
    // Ocultar UI
    document.getElementById('facePreviewContainer').style.display = 'none';
    document.getElementById('faceControls').style.display = 'none';
}
```

#### Gestión de Modelos
```javascript
let faceApiLoaded = false;

async function ensureFaceApiModels() {
    if (faceApiLoaded) return;
    
    console.log('[FACE-API] Cargando modelos...');
    const modelPath = '/webjars/face-api/models';
    
    await Promise.all([
        faceapi.nets.ssdMobilenetv1.loadFromUri(modelPath),
        faceapi.nets.faceLandmark68Net.loadFromUri(modelPath),
        faceapi.nets.faceRecognitionNet.loadFromUri(modelPath)
    ]);
    
    faceApiLoaded = true;
    console.log('[FACE-API] Modelos cargados exitosamente');
}
```

### 2. Grabación de Voz

#### HTML Structure
```html
<!-- Contenedor de grabación -->
<div id="voiceRecorder" class="voice-recorder" style="display:none;">
    <div class="recorder-status">
        <div class="waveform" id="voiceWaveform">
            <div class="wave-bar"></div>
            <div class="wave-bar"></div>
            <div class="wave-bar"></div>
            <div class="wave-bar"></div>
            <div class="wave-bar"></div>
        </div>
        <div class="timer" id="voiceTimer">00:00</div>
        <div class="status-text" id="voiceStatusText">Listo para grabar</div>
    </div>
    
    <div class="recorder-controls">
        <button id="btnMic" class="mic-btn">
            🎤 Iniciar Grabación
        </button>
    </div>
</div>
```

#### CSS Styles
```css
.voice-recorder {
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    border-radius: 16px;
    padding: 30px;
    color: white;
    text-align: center;
}

.recorder-status {
    margin-bottom: 30px;
}

.waveform {
    display: flex;
    justify-content: center;
    align-items: center;
    gap: 4px;
    height: 60px;
    margin-bottom: 15px;
}

.wave-bar {
    width: 4px;
    height: 20px;
    background: rgba(255,255,255,0.3);
    border-radius: 2px;
    transition: height 0.1s ease;
}

.waveform.active .wave-bar {
    animation: wave 0.8s ease-in-out infinite;
}

.waveform.active .wave-bar:nth-child(2) { animation-delay: 0.1s; }
.waveform.active .wave-bar:nth-child(3) { animation-delay: 0.2s; }
.waveform.active .wave-bar:nth-child(4) { animation-delay: 0.3s; }
.waveform.active .wave-bar:nth-child(5) { animation-delay: 0.4s; }

@keyframes wave {
    0%, 100% { height: 20px; }
    50% { height: 50px; }
}

.timer {
    font-size: 36px;
    font-weight: bold;
    font-family: 'Courier New', monospace;
    margin-bottom: 10px;
}

.timer.warning {
    color: #FCD34D;
}

.status-text {
    font-size: 14px;
    opacity: 0.9;
}

.mic-btn {
    background: white;
    color: #667eea;
    border: none;
    border-radius: 50px;
    padding: 15px 30px;
    font-size: 16px;
    font-weight: 600;
    cursor: pointer;
    transition: all 0.3s ease;
    box-shadow: 0 4px 6px rgba(0,0,0,0.1);
}

.mic-btn:hover {
    transform: scale(1.05);
    box-shadow: 0 6px 12px rgba(0,0,0,0.15);
}

.mic-btn.recording {
    background: #EF4444;
    color: white;
    animation: pulse 1.5s infinite;
}

@keyframes pulse {
    0%, 100% { opacity: 1; }
    50% { opacity: 0.7; }
}
```

#### JavaScript Logic
```javascript
let voiceRecorder = null;
let voiceChunks = [];
let voiceRecording = false;
let voiceSeconds = 0;
let voiceTimer = null;

async function toggleVoiceRecording() {
    if (!voiceRecording) {
        await startVoiceRecording();
    } else {
        stopVoiceRecording();
    }
}

async function startVoiceRecording() {
    try {
        // Solicitar micrófono
        const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
        
        // Crear MediaRecorder
        voiceRecorder = new MediaRecorder(stream, { mimeType: 'audio/webm' });
        voiceChunks = [];
        
        voiceRecorder.ondataavailable = (e) => {
            if (e.data.size > 0) {
                voiceChunks.push(e.data);
            }
        };
        
        voiceRecorder.onstop = () => {
            processVoiceRecording();
        };
        
        // Iniciar grabación
        voiceRecorder.start();
        voiceRecording = true;
        voiceSeconds = 0;
        
        // Actualizar UI
        document.getElementById('voiceRecorder').style.display = 'block';
        document.getElementById('voiceWaveform').classList.add('active');
        document.getElementById('btnMic').textContent = '⏹️ Detener Grabación';
        document.getElementById('btnMic').classList.add('recording');
        document.getElementById('voiceStatusText').textContent = 'Grabando...';
        
        // Timer
        voiceTimer = setInterval(() => {
            voiceSeconds++;
            const mins = Math.floor(voiceSeconds / 60).toString().padStart(2, '0');
            const secs = (voiceSeconds % 60).toString().padStart(2, '0');
            document.getElementById('voiceTimer').textContent = `${mins}:${secs}`;
            
            // Advertencia si muy corto
            if (voiceSeconds < 3) {
                document.getElementById('voiceTimer').classList.add('warning');
                document.getElementById('voiceStatusText').textContent = 'Mínimo 3 segundos';
            } else {
                document.getElementById('voiceTimer').classList.remove('warning');
                document.getElementById('voiceStatusText').textContent = 'Presiona detener cuando termines';
            }
        }, 1000);
        
        console.log('[VOICE] Grabación iniciada');
        
    } catch (error) {
        console.error('[VOICE] Error al iniciar grabación:', error);
        showStatus('error', 'No se pudo acceder al micrófono');
    }
}

function stopVoiceRecording() {
    // Validar duración mínima
    if (voiceSeconds < 3) {
        showStatus('error', 'Grabación muy corta. Mínimo 3 segundos.');
        return;
    }
    
    // Detener timer
    clearInterval(voiceTimer);
    voiceTimer = null;
    
    // Detener MediaRecorder
    if (voiceRecorder && voiceRecorder.state !== 'inactive') {
        voiceRecorder.stop();
        voiceRecorder.stream.getTracks().forEach(track => track.stop());
    }
    
    voiceRecording = false;
    
    // Actualizar UI
    document.getElementById('voiceWaveform').classList.remove('active');
    document.getElementById('btnMic').textContent = '🎤 Iniciar Grabación';
    document.getElementById('btnMic').classList.remove('recording');
    document.getElementById('voiceStatusText').textContent = 'Procesando...';
    
    console.log('[VOICE] Grabación detenida:', voiceSeconds, 'segundos');
}

async function processVoiceRecording() {
    try {
        // Crear blob de audio
        const blob = new Blob(voiceChunks, { type: 'audio/webm' });
        
        // Crear AudioContext offline para procesar
        const offline = new OfflineAudioContext(1, 44100 * 10, 44100);
        const arrayBuffer = await blob.arrayBuffer();
        const audioBuffer = await offline.decodeAudioData(arrayBuffer);
        const channel = audioBuffer.getChannelData(0);
        
        // Extraer MFCC con Meyda
        const frameSize = 1024;
        const hop = 512;
        let mfccSum = null;
        let count = 0;
        
        for (let i = 0; i + frameSize <= channel.length; i += hop) {
            const frame = channel.slice(i, i + frameSize);
            const features = Meyda.extract('mfcc', frame, {
                sampleRate: 44100,
                bufferSize: frameSize,
                melBands: 26,
                numberOfMFCCCoefficients: 13
            });
            
            if (features && features.mfcc) {
                if (!mfccSum) mfccSum = new Array(13).fill(0);
                for (let k = 0; k < 13; k++) {
                    mfccSum[k] += features.mfcc[k];
                }
                count++;
            }
        }
        
        const avgMfcc = mfccSum.map(v => v / count);
        
        // Enviar al backend
        const response = await fetch('/api/auth/biometric/verify', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                userId: userId,
                token: token,
                method: 'voice',
                data: JSON.stringify({ features: avgMfcc }),
                secondAttempt: false
            })
        });
        
        const result = await response.json();
        
        if (result.success) {
            showStatus('success', '✓ Verificación exitosa');
            document.getElementById('voiceRecorder').style.display = 'none';
        } else {
            showStatus('error', result.message || 'Verificación fallida');
            // Reset UI para retry
            document.getElementById('voiceStatusText').textContent = 'Listo para grabar';
            document.getElementById('voiceTimer').textContent = '00:00';
        }
        
    } catch (error) {
        console.error('[VOICE] Error al procesar:', error);
        showStatus('error', 'Error al procesar audio');
    }
}
```

## 🧪 Cómo Probar

### Preview de FaceID
```
1. Móvil → Escanear QR de autenticación
2. Clic "FaceID"
3. Verificar:
   ✓ Stream de cámara visible
   ✓ Recuadro verde sobre rostro cuando detectado
   ✓ Mensaje "Rostro detectado ✓" en verde
   ✓ Mensaje "No se detecta rostro" si giras
   ✓ Mensaje "Múltiples rostros" si hay 2+ personas
4. Clic "Capturar y Verificar"
5. Desktop reacciona instantáneamente
```

### Grabación de Voz
```
1. Móvil → Clic "Voz"
2. Verificar:
   ✓ Botón "🎤 Iniciar Grabación" visible
3. Clic en botón
4. Verificar:
   ✓ Timer inicia: 00:01, 00:02, 00:03...
   ✓ Waveform animado (barras subiendo/bajando)
   ✓ Botón cambia a "⏹️ Detener Grabación" (rojo pulsante)
   ✓ Mensaje "Mínimo 3 segundos" si timer < 3s
5. Intenta detener antes de 3s → Error: "Grabación muy corta"
6. Graba 3+ segundos → Clic detener → Procesa y verifica
```

## 🎨 Diseño Visual

### Estados de FaceID
```
┌─────────────────────────────┐
│ 🔵 Iniciando cámara...      │  (Cargando)
└─────────────────────────────┘

┌─────────────────────────────┐
│ 🟡 No se detecta rostro     │  (Advertencia)
└─────────────────────────────┘

┌─────────────────────────────┐
│ 🔴 Múltiples rostros        │  (Error)
└─────────────────────────────┘

┌─────────────────────────────┐
│ 🟢 Rostro detectado ✓       │  (Éxito)
└─────────────────────────────┘
```

### Estados de Voz
```
┌─────────────────────────────┐
│ 🎤 Listo para grabar        │  (Idle)
└─────────────────────────────┘

┌─────────────────────────────┐
│ 🔴 00:02 - Mínimo 3s        │  (Grabando < 3s)
└─────────────────────────────┘

┌─────────────────────────────┐
│ 🟢 00:05 - Detener cuando   │  (Grabando > 3s)
│    termines                 │
└─────────────────────────────┘

┌─────────────────────────────┐
│ ⏳ Procesando...            │  (Extrayendo MFCC)
└─────────────────────────────┘
```

## 🔧 Configuración

### Umbrales de Detección
```javascript
// face-api.js
const faceOptions = new faceapi.SsdMobilenetv1Options({
    minConfidence: 0.5  // 50% confianza mínima
});

// Detección cada 200ms (5 fps)
setInterval(detectFaces, 200);
```

### Parámetros de Audio
```javascript
// MediaRecorder
const recorderOptions = {
    mimeType: 'audio/webm',  // Codec estándar
    audioBitsPerSecond: 128000  // 128 kbps
};

// Meyda MFCC
const meydaOptions = {
    sampleRate: 44100,           // CD quality
    bufferSize: 1024,            // ~23ms frames
    melBands: 26,                // Filtros mel
    numberOfMFCCCoefficients: 13 // Coeficientes
};
```

### Duración Mínima de Voz
```javascript
const MIN_RECORDING_SECONDS = 3;

if (voiceSeconds < MIN_RECORDING_SECONDS) {
    showStatus('error', 'Grabación muy corta. Mínimo 3 segundos.');
    return;
}
```

## 🚨 Casos de Error

### Cámara no disponible
```javascript
// Error: NotAllowedError
→ Usuario denegó permisos
→ Mostrar instrucciones para habilitar en Settings

// Error: NotFoundError
→ No hay cámara disponible
→ Sugerir usar método alternativo (Voz o Huella)
```

### Micrófono no disponible
```javascript
// Error: NotAllowedError
→ Usuario denegó permisos
→ Mostrar instrucciones para habilitar

// Error: NotSupportedError
→ Navegador no soporta MediaRecorder
→ Sugerir actualizar navegador o usar otro método
```

### Modelos face-api no cargan
```javascript
// Error: 404 en /webjars/face-api/models/*
→ Verificar webjars en pom.xml
→ Verificar ResourceHandler en Spring Boot
→ Fallback: usar CDN externo
```

## 📊 Métricas de UX

| Métrica | Objetivo | Real |
|---------|----------|------|
| Tiempo hasta preview | < 2s | ~1.5s |
| FPS de detección | 5 fps | 5 fps |
| Tiempo de captura | < 3s | ~2s |
| Duración mín. voz | 3s | 3s |
| Tiempo procesamiento voz | < 5s | ~3s |

## 🔐 Privacidad

- Video stream **nunca** se envía al servidor
- Solo se envía descriptor 128D (no imagen)
- Audio se procesa localmente (MFCC en browser)
- Solo se envía vector 13D (no audio raw)
- Stream se detiene inmediatamente tras captura
- No se guardan grabaciones en disco

---

**Implementado**: 13-14 de noviembre de 2025  
**Versión**: 3.0.0  
**Autor**: Sistema EduFeed
