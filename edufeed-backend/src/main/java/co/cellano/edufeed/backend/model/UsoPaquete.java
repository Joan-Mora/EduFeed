package co.cellano.edufeed.backend.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "usos_paquete")
public class UsoPaquete {
    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paquete_id", nullable = false)
    private PaquetePago paquete;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "acceso_id")
    private Acceso acceso;

    @Column(name = "usado_en", nullable = false)
    private OffsetDateTime usadoEn = OffsetDateTime.now();

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public PaquetePago getPaquete() {
        return paquete;
    }

    public void setPaquete(PaquetePago paquete) {
        this.paquete = paquete;
    }

    public Acceso getAcceso() {
        return acceso;
    }

    public void setAcceso(Acceso acceso) {
        this.acceso = acceso;
    }

    public OffsetDateTime getUsadoEn() {
        return usadoEn;
    }

    public void setUsadoEn(OffsetDateTime usadoEn) {
        this.usadoEn = usadoEn;
    }
}
