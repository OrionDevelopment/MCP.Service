package org.modmappings.mmms.repository.repositories.core.release;

import org.modmappings.mmms.er2dbc.data.access.strategy.ExtendedDataAccessStrategy;
import org.modmappings.mmms.er2dbc.data.statements.join.JoinSpec;
import org.modmappings.mmms.er2dbc.data.statements.mapper.ExtendedStatementMapper;
import org.modmappings.mmms.er2dbc.data.statements.select.SelectSpecWithJoin;
import org.modmappings.mmms.repository.model.core.release.ReleaseDMO;
import org.modmappings.mmms.repository.repositories.ModMappingR2DBCRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.convert.R2dbcConverter;
import org.springframework.data.r2dbc.core.DatabaseClient;
import org.springframework.data.r2dbc.core.PreparedOperation;
import org.springframework.data.relational.core.sql.Functions;
import org.springframework.data.relational.core.sql.Table;
import org.springframework.data.relational.repository.query.RelationalEntityInformation;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

import static org.modmappings.mmms.er2dbc.data.statements.criteria.ColumnBasedCriteria.*;

/**
 * Represents a repository which can provide and store {@link ReleaseDMO} objects.
 */
@Repository
public class ReleaseRepository extends ModMappingR2DBCRepository<ReleaseDMO> {

    public ReleaseRepository(RelationalEntityInformation<ReleaseDMO, UUID> entity, DatabaseClient databaseClient, R2dbcConverter converter, ExtendedDataAccessStrategy accessStrategy) {
        super(entity, databaseClient, converter, accessStrategy);
    }

    /**
     * Finds all releases which match the search criteria if they are supplied.
     * Supply null to anyone of them to ignore the search.
     *
     * @param nameRegex     The regex to filter the name on-
     * @param gameVersionId The id of the game version to filter releases on.
     * @param mappingTypeId The id of the mapping type to filter releases on.
     * @param isSnapshot    Indicate if snapshots should be included or not.
     * @param mappingId     The id of the mapping to filter releases on.
     * @param userId        The id of the creating user to filter releases on.
     * @param pageable      The paging information for the query.
     * @return All releases which match the given criteria.
     */
    public Mono<Page<ReleaseDMO>> findAllByOld(
            final String nameRegex,
            final UUID gameVersionId,
            final UUID mappingTypeId,
            final Boolean isSnapshot,
            final UUID mappingId,
            final UUID userId,
            final Pageable pageable) {
        return createPagedAllByRequest(
                nameRegex, gameVersionId, mappingTypeId, isSnapshot, mappingId, userId, pageable
        );
    }

    private Mono<Page<ReleaseDMO>> createPagedAllByRequest(
            final String nameRegex,
            final UUID gameVersionId,
            final UUID mappingTypeId,
            final Boolean isSnapshot,
            final UUID mappingId,
            final UUID userId,
            final Pageable pageable
    ) {
        return createPagedStarRequest(
                selectSpec -> selectSpec.withJoin(
                        JoinSpec.join("release_component", "rc")
                                .withOn(on(reference("id")).is(reference("rc", "release_id")))
                    )
                    .withCriteria(
                            where(
                                    parameter(nameRegex)).isNull().or(reference("r", "name")).matches(parameter(nameRegex))
                                    .and(parameter(gameVersionId)).isNull().or(reference("r", "game_version_id")).is(parameter(gameVersionId))
                                    .and(parameter(mappingTypeId)).isNull().or(reference("r", "mapping_type_id")).is(parameter(mappingTypeId))
                                    .and(parameter(isSnapshot)).isNull().or(reference("r", "is_snapshot")).is(parameter(isSnapshot))
                                    .and(parameter(mappingId)).isNull().or(reference("rc", "mapping_id")).is(parameter(mappingId))
                                    .and(parameter(userId)).isNull().or(reference("r", "created_by")).is(parameter(userId))
                    ),
                pageable
        );
    }
}
