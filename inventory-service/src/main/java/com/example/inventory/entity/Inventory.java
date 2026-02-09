package com.example.inventory.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "inventory")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String productName;

    private int stock;
}
