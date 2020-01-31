package org.modmappings.mmms.api.services.core;

import org.modmappings.mmms.api.model.core.MappingTypeDTO;
import org.modmappings.mmms.api.services.utils.exceptions.EntryNotFoundException;
import org.modmappings.mmms.api.services.utils.exceptions.InsertionFailureDueToDuplicationException;
import org.modmappings.mmms.api.services.utils.exceptions.NoEntriesFoundException;
import org.modmappings.mmms.api.services.utils.user.UserLoggingService;
import org.modmappings.mmms.repository.model.core.MappingTypeDMO;
import org.modmappings.mmms.repository.repositories.core.mappingtypes.MappingTypeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Business layer service which handles the interactions of the API with the DataLayer.
 * <p>
 * This services validates data as well as converts between the API models as well as the data models.
 * <p>
 * This services however does not validate if a given user is authorized to execute a given action.
 * It only validates the interaction from a data perspective.
 * <p>
 * The caller is to make sure that any interaction with this service is authorized, for example by checking
 * against a role that a user needs to have.
 */
@Component
public class MappingTypeService {

    private final Logger logger = LoggerFactory.getLogger(MappingTypeService.class);
    private final MappingTypeRepository repository;
    private final UserLoggingService userLoggingService;

    public MappingTypeService(MappingTypeRepository repository, UserLoggingService userLoggingService) {
        this.repository = repository;
        this.userLoggingService = userLoggingService;
    }

    /**
     * Looks up a mapping type with a given id.
     *
     * @param id The id to look the mapping type up for.
     * @param externallyVisibleOnly Indicates if only externally visible mapping types should be returned.
     * @return A {@link Mono} containing the requested mapping type or a errored {@link Mono} that indicates a failure.
     */
    public Mono<MappingTypeDTO> getBy(
            final UUID id,
            final boolean externallyVisibleOnly
    ) {
        return repository.findById(id, externallyVisibleOnly)
                .doFirst(() -> logger.debug("Looking up a mapping type by id: {}", id))
                .map(this::toDTO)
                .doOnNext(dto -> logger.debug("Found mapping type: {} with id: {}", dto.getName(), dto.getId()))
                .switchIfEmpty(Mono.error(new EntryNotFoundException(id, "MappingType")));
    }

    /**
     * Looks up multiple mapping types, that match the search criteria.
     * The returned order is newest to oldest.
     *
     * @param nameRegex The regular expression against which the name of the mapping type is matched.
     * @param editable Indicates if editable mapping types need to be included, null indicates do not care.
     * @param externallyVisibleOnly Indicator if only externally visible mapping types should be returned.
     * @param pageable The paging and sorting information.
     * @return A {@link Flux} with the mapping types, or an errored {@link Flux} that indicates a failure.
     */
    public Mono<Page<MappingTypeDTO>> getAll(
            final String nameRegex,
            final Boolean editable,
            final boolean externallyVisibleOnly,
            final Pageable pageable
    ) {
        return repository.findAllBy(
                nameRegex,
                editable,
                externallyVisibleOnly,
                pageable)
                .doFirst(() -> logger.debug("Looking up mapping types in search mode. Using parameters: {}, {}", nameRegex, editable))
                .flatMap(page -> Flux.fromIterable(page)
                        .map(this::toDTO)
                        .collectList()
                        .map(mappingTypes -> (Page<MappingTypeDTO>) new PageImpl<>(mappingTypes, page.getPageable(), page.getTotalElements())))
                .doOnNext(page -> logger.debug("Found mapping types: {}", page))
                .switchIfEmpty(Mono.error(new NoEntriesFoundException("MappingType")));
    }

    /**
     * Deletes a given mapping type if it exists.
     *
     * @param id The id of the mapping type that should be deleted.
     * @param externallyVisibleOnly Indicator if only externally visible mapping types should be taken into account.
     * @return A {@link Mono} indicating success or failure.
     */
    public Mono<Void> deleteBy(
            final UUID id,
            final boolean externallyVisibleOnly,
            final Supplier<UUID> userIdSupplier
    ) {
        return repository
                .findById(id, externallyVisibleOnly)
                .flatMap(dmo -> repository.deleteById(id)
                        .doFirst(() -> userLoggingService.warn(logger, userIdSupplier, String.format("Deleting mapping type with id: %s", id)))
                        .doOnNext(aVoid -> userLoggingService.warn(logger, userIdSupplier, String.format("Deleted mapping type with id: %s", id))));

    }

    /**
     * Creates a new mapping type from a DTO and saves it in the repository.
     *
     * @param newMappingType The dto to create a new mapping type from.
     * @param userIdSupplier A provider that gives access to the user id of the currently interacting user or service.
     * @return A {@link Mono} that indicates success or failure.
     */
    public Mono<MappingTypeDTO> create(
            final MappingTypeDTO newMappingType,
            final Supplier<UUID> userIdSupplier
    ) {
        return Mono.just(newMappingType)
                .doFirst(() -> userLoggingService.warn(logger, userIdSupplier, String.format("Creating new mapping type: %s", newMappingType.getName())))
                .map(dto -> this.toNewDMO(dto, userIdSupplier))
                .flatMap(repository::save)
                .map(this::toDTO)
                .doOnNext(dmo -> userLoggingService.warn(logger, userIdSupplier, String.format("Created new mapping type: %s with id: %s", dmo.getName(), dmo.getId())))
                .onErrorResume(throwable -> throwable.getMessage().contains("duplicate key value violates unique constraint \"IX_mapping_type_name\""), dive -> Mono.error(new InsertionFailureDueToDuplicationException("MappingType", "Name")));
    }

    /**
     * Updates an existing mapping type with the data in the dto and saves it in the repo.
     *
     * @param idToUpdate The id of the dmo that should be updated with the data in the dto.
     * @param newMappingType The dto to update the data in the dmo with.
     * @param externallyVisibleOnly Indicator if only externally visible mapping types should be considered.
     * @param userIdSupplier A provider that gives access to the user id of the currently interacting user or service.
     * @return A {@link Mono} that indicates success or failure.
     */
    public Mono<MappingTypeDTO> update(
            final UUID idToUpdate,
            final MappingTypeDTO newMappingType,
            final boolean externallyVisibleOnly,
            final Supplier<UUID> userIdSupplier
    ) {
        return repository.findById(idToUpdate, externallyVisibleOnly)
                .doFirst(() -> userLoggingService.warn(logger, userIdSupplier, String.format("Updating mapping type: %s", idToUpdate)))
                .switchIfEmpty(Mono.error(new EntryNotFoundException(newMappingType.getId(), "MappingType")))
                .doOnNext(dmo -> userLoggingService.warn(logger, userIdSupplier, String.format("Updating db mapping type: %s with id: %s, and data: %s", dmo.getName(), dmo.getId(), newMappingType)))
                .filter(MappingTypeDMO::isVisible)
                .doOnNext(dmo -> this.updateDMO(newMappingType, dmo)) //We use doOnNext here since this maps straight into the existing dmo that we just pulled from the DB to update.
                .doOnNext(dmo -> userLoggingService.warn(logger, userIdSupplier, String.format("Updated db mapping type to: %s", dmo)))
                .flatMap(dmo -> repository.save(dmo)
                        .onErrorResume(throwable -> throwable.getMessage().contains("duplicate key value violates unique constraint \"IX_mapping_type_name\""), dive -> Mono.error(new InsertionFailureDueToDuplicationException("MappingType", "Name"))))
                .map(this::toDTO)
                .doOnNext(dto -> userLoggingService.warn(logger, userIdSupplier, String.format("Updated mapping type: %s with id: %s, to data: %s", dto.getName(), dto.getId(), dto)));
    }

    private MappingTypeDTO toDTO(MappingTypeDMO dmo) {
        return new MappingTypeDTO(
                dmo.getId(),
                dmo.getCreatedBy(),
                dmo.getCreatedOn(),
                dmo.getName(),
                dmo.isEditable()
        );
    }

    private MappingTypeDMO toNewDMO(MappingTypeDTO dto, Supplier<UUID> userIdSupplier) {
        return new MappingTypeDMO(
                userIdSupplier.get(),
                dto.getName(),
                true,
                dto.isEditable(),
                dto.getStateIn(),
                dto.getStateOut()
        );
    }

    private void updateDMO(MappingTypeDTO dto, MappingTypeDMO dmo) {
        dmo.setName(dto.getName());
        dmo.setEditable(dmo.isEditable());
        dmo.setStateIn(dmo.getStateIn());
        dmo.setStateOut(dmo.getStateOut());
    }
}
