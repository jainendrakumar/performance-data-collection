package com.example.qlogserver.service;

import com.example.qlogserver.model.TransactionRecord;
import com.example.qlogserver.repository.TransactionRecordRepository;
import jakarta.persistence.criteria.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Service for querying and retrieving TransactionRecord data.
 * Supports filtering and pagination.
 */
@Service
public class TransactionService {

    private static final Logger log = LoggerFactory.getLogger(TransactionService.class);

    private final TransactionRecordRepository repository;

    @Autowired
    public TransactionService(TransactionRecordRepository repository) {
        this.repository = repository;
    }

    /**
     * Finds transactions based on filter criteria with pagination and sorting.
     *
     * @param filters  A map containing filter criteria (e.g., "transactionKind", "status", "environmentContext").
     * @param pageable Pageable object for pagination and sorting.
     * @return A Page of TransactionRecord objects matching the criteria.
     */
    public Page<TransactionRecord> findTransactions(Map<String, String> filters, Pageable pageable) {
        log.debug("Finding transactions with filters: {} and pageable: {}", filters, pageable);
        Specification<TransactionRecord> spec = buildSpecification(filters);
        return repository.findAll(spec, pageable);
    }

    /**
     * Finds all transactions matching the filter criteria (no pagination).
     * Used primarily for reporting.
     *
     * @param filters A map containing filter criteria.
     * @return A List of all TransactionRecord objects matching the criteria.
     */
    public List<TransactionRecord> findAllTransactions(Map<String, String> filters) {
        log.debug("Finding all transactions with filters: {}", filters);
        Specification<TransactionRecord> spec = buildSpecification(filters);
        return repository.findAll(spec);
    }

    /**
     * Builds a JPA Specification based on the provided filter map.
     *
     * @param filters A map of filter criteria.
     * @return A Specification<TransactionRecord> object.
     */
    private Specification<TransactionRecord> buildSpecification(Map<String, String> filters) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // --- Add predicates based on filters --- 

            // Example: Filter by Transaction Kind
            if (StringUtils.hasText(filters.get("transactionKind"))) {
                predicates.add(criteriaBuilder.equal(root.get("transactionKind"), filters.get("transactionKind")));
            }

            // Example: Filter by Status
            if (StringUtils.hasText(filters.get("status"))) {
                predicates.add(criteriaBuilder.equal(root.get("status"), filters.get("status")));
            }

            // Example: Filter by Min Length
            if (StringUtils.hasText(filters.get("minLength"))) {
                try {
                    double minLength = Double.parseDouble(filters.get("minLength"));
                    predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("length"), minLength));
                } catch (NumberFormatException e) {
                    log.warn("Invalid minLength filter value: {}", filters.get("minLength"));
                }
            }

            // Example: Filter by Max Length
            if (StringUtils.hasText(filters.get("maxLength"))) {
                try {
                    double maxLength = Double.parseDouble(filters.get("maxLength"));
                    predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("length"), maxLength));
                } catch (NumberFormatException e) {
                    log.warn("Invalid maxLength filter value: {}", filters.get("maxLength"));
                }
            }

            // Example: Filter by Username
            if (StringUtils.hasText(filters.get("username"))) {
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("username")), "%" + filters.get("username").toLowerCase() + "%"));
            }

            // *** NEW: Filter by Environment Context ***
            if (StringUtils.hasText(filters.get("environmentContext"))) {
                predicates.add(criteriaBuilder.equal(root.get("environmentContext"), filters.get("environmentContext")));
            }

            // Add more filters as needed (e.g., date range based on startTimeRaw/endTimeRaw - requires careful handling)

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Retrieves a list of distinct environment contexts present in the database.
     *
     * @return List of distinct environment context strings.
     */
    public List<String> findDistinctEnvironmentContexts() {
        return repository.findDistinctEnvironmentContexts();
    }
}

