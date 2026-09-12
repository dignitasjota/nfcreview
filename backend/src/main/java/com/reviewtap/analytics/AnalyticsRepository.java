package com.reviewtap.analytics;

import com.reviewtap.interaction.Interaction;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.QueryHint;

/** Agregaciones en SQL: nunca se cargan interacciones en memoria para contarlas. */
public interface AnalyticsRepository extends Repository<Interaction, UUID> {

    interface DeviceCount {
        UUID getDeviceId();

        long getTotal();
    }

    interface Totals {
        long getTotal();

        long getNfc();

        long getQr();

        long getUnknown();
    }

    interface DayCount {
        String getDay();

        long getTotal();

        long getNfc();

        long getQr();
    }

    interface DeviceStats {
        UUID getDeviceId();

        String getName();

        String getLocation();

        Boolean getActive();

        long getNfc();

        long getQr();

        long getUnknown();

        long getTotal();
    }

    interface BusinessCount {
        UUID getBusinessId();

        String getName();

        long getTotal();
    }

    @Query(value = """
            select device_id as deviceId, count(*) as total
            from interaction
            where device_id in (:ids) and created_at >= :since
            group by device_id
            """, nativeQuery = true)
    List<DeviceCount> countByDeviceSince(@Param("ids") List<UUID> ids, @Param("since") Instant since);

    @Query(value = """
            select count(*) as total,
                   count(*) filter (where i.interaction_type = 'NFC') as nfc,
                   count(*) filter (where i.interaction_type = 'QR') as qr,
                   count(*) filter (where i.interaction_type = 'UNKNOWN') as unknown
            from interaction i
            join device d on d.id = i.device_id
            where d.business_id = :businessId and i.created_at >= :from and i.created_at < :to
            """, nativeQuery = true)
    Totals totals(@Param("businessId") UUID businessId, @Param("from") Instant from, @Param("to") Instant to);

    @Query(value = """
            select to_char((i.created_at at time zone :tz)::date, 'YYYY-MM-DD') as day,
                   count(*) as total,
                   count(*) filter (where i.interaction_type = 'NFC') as nfc,
                   count(*) filter (where i.interaction_type = 'QR') as qr
            from interaction i
            join device d on d.id = i.device_id
            where d.business_id = :businessId and i.created_at >= :from and i.created_at < :to
            group by 1
            order by 1
            """, nativeQuery = true)
    List<DayCount> timeline(@Param("businessId") UUID businessId, @Param("from") Instant from,
            @Param("to") Instant to, @Param("tz") String tz);

    @Query(value = """
            select d.id as deviceId, d.name as name, d.location_description as location, d.active as active,
                   count(i.id) filter (where i.interaction_type = 'NFC') as nfc,
                   count(i.id) filter (where i.interaction_type = 'QR') as qr,
                   count(i.id) filter (where i.interaction_type = 'UNKNOWN') as unknown,
                   count(i.id) as total
            from device d
            left join interaction i on i.device_id = d.id and i.created_at >= :from and i.created_at < :to
            where d.business_id = :businessId
            group by d.id, d.name, d.location_description, d.active, d.created_at
            order by total desc, d.created_at asc
            """, nativeQuery = true)
    List<DeviceStats> deviceStats(@Param("businessId") UUID businessId, @Param("from") Instant from,
            @Param("to") Instant to);

    @Query("""
            select new com.reviewtap.analytics.RecentInteraction(
                i.createdAt, d.id, d.name, d.locationDescription, i.interactionType)
            from Interaction i join Device d on d.id = i.deviceId
            where d.business.id = :businessId
            order by i.createdAt desc
            """)
    List<RecentInteraction> recent(@Param("businessId") UUID businessId, Pageable pageable);

    @Query("""
            select new com.reviewtap.analytics.ExportRow(
                i.createdAt, d.name, d.locationDescription, i.interactionType)
            from Interaction i join Device d on d.id = i.deviceId
            where d.business.id = :businessId and i.createdAt >= :from and i.createdAt < :to
            order by i.createdAt asc
            """)
    @QueryHints(@QueryHint(name = "org.hibernate.fetchSize", value = "500"))
    Stream<ExportRow> streamForExport(@Param("businessId") UUID businessId, @Param("from") Instant from,
            @Param("to") Instant to);

    @Query("""
            select new com.reviewtap.analytics.ExportRow(
                i.createdAt, d.name, d.locationDescription, i.interactionType)
            from Interaction i join Device d on d.id = i.deviceId
            where d.business.id = :businessId and d.id = :deviceId
              and i.createdAt >= :from and i.createdAt < :to
            order by i.createdAt asc
            """)
    @QueryHints(@QueryHint(name = "org.hibernate.fetchSize", value = "500"))
    Stream<ExportRow> streamForExportByDevice(@Param("businessId") UUID businessId,
            @Param("deviceId") UUID deviceId, @Param("from") Instant from, @Param("to") Instant to);

    // ---- Globales (ADMIN) ----

    @Query(value = """
            select count(*) as total,
                   count(*) filter (where interaction_type = 'NFC') as nfc,
                   count(*) filter (where interaction_type = 'QR') as qr,
                   count(*) filter (where interaction_type = 'UNKNOWN') as unknown
            from interaction
            where created_at >= :from and created_at < :to
            """, nativeQuery = true)
    Totals globalTotals(@Param("from") Instant from, @Param("to") Instant to);

    @Query(value = """
            select to_char((created_at at time zone :tz)::date, 'YYYY-MM-DD') as day,
                   count(*) as total,
                   count(*) filter (where interaction_type = 'NFC') as nfc,
                   count(*) filter (where interaction_type = 'QR') as qr
            from interaction
            where created_at >= :from and created_at < :to
            group by 1
            order by 1
            """, nativeQuery = true)
    List<DayCount> globalTimeline(@Param("from") Instant from, @Param("to") Instant to, @Param("tz") String tz);

    @Query(value = """
            select b.id as businessId, b.name as name, count(i.id) as total
            from business b
            join device d on d.business_id = b.id
            join interaction i on i.device_id = d.id and i.created_at >= :from and i.created_at < :to
            group by b.id, b.name
            order by total desc
            limit :limit
            """, nativeQuery = true)
    List<BusinessCount> topBusinesses(@Param("from") Instant from, @Param("to") Instant to, @Param("limit") int limit);
}
