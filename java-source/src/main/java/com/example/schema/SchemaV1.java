package com.example.schema;

import com.google.common.collect.ImmutableList;
import net.corda.core.schemas.MappedSchema;
import net.corda.core.schemas.PersistentState;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.UUID;

/**
 * A schema.
 */
public class SchemaV1 extends MappedSchema {
    public SchemaV1() {
        super(Schema.class, 1, ImmutableList.of(PersistentIOU.class));
    }

    @Entity
    @Table(name = "states")
    public static class PersistentIOU extends PersistentState {
        @Column(name = "couponInitiator") private final String couponInitiator;
        @Column(name = "couponVendor") private final String couponVendor;
        @Column(name = "value") private final int value;
        @Column(name = "couponId") private final UUID couponId;


        public PersistentIOU(String couponInitiator, String couponVendor, int value, UUID couponId) {
            this.couponInitiator = couponInitiator;
            this.couponVendor = couponVendor;
            this.value = value;
            this.couponId = couponId;
        }

        // Default constructor required by hibernate.
        public PersistentIOU() {
            this.couponInitiator = null;
            this.couponVendor = null;
            this.value = 0;
            this.couponId = null;
        }

        public String getCouponInitiator() {
            return couponInitiator;
        }

        public String getCouponVendor() {
            return couponVendor;
        }

        public int getValue() {
            return value;
        }

        public UUID getCouponId() {
            return couponId;
        }
    }
}