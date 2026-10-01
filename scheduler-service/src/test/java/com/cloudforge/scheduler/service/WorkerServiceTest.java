package com.cloudforge.scheduler.service;

import com.cloudforge.common.model.WorkerStatus;
import com.cloudforge.scheduler.dto.WorkerRegistrationRequest;
import com.cloudforge.scheduler.dto.WorkerResponse;
import com.cloudforge.scheduler.entity.WorkerEntity;
import com.cloudforge.scheduler.exception.WorkerNotFoundException;
import com.cloudforge.scheduler.repository.WorkerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkerServiceTest {

    @Mock
    private WorkerRepository workerRepository;

    @Test
    void registerWorker_withNewId_persistsWorkerAsRegistering() {
        WorkerService workerService = new WorkerService(workerRepository);

        WorkerRegistrationRequest request = new WorkerRegistrationRequest();
        request.setCpuCapacity(4.0);
        request.setMemoryCapacity(8192L);

        when(workerRepository.findById("worker-1")).thenReturn(Optional.empty());
        when(workerRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        WorkerRegistrationResult result = workerService.registerWorker("worker-1", request);

        ArgumentCaptor<WorkerEntity> captor = ArgumentCaptor.forClass(WorkerEntity.class);
        verify(workerRepository).save(captor.capture());
        WorkerEntity saved = captor.getValue();

        assertThat(saved.getId()).isEqualTo("worker-1");
        assertThat(saved.getCpuCapacity()).isEqualTo(4.0);
        assertThat(saved.getMemoryCapacity()).isEqualTo(8192L);
        assertThat(saved.getCpuAvailable()).isEqualTo(4.0);
        assertThat(saved.getMemoryAvailable()).isEqualTo(8192L);
        assertThat(saved.getStatus()).isEqualTo(WorkerStatus.REGISTERING);
        assertThat(saved.getRegisteredAt()).isNotNull();

        assertThat(result.alreadyRegistered()).isFalse();
        assertThat(result.response().getId()).isEqualTo("worker-1");
    }

    @Test
    void registerWorker_withExistingId_updatesCapacityAndKeepsOriginalRegisteredAt() {
        WorkerService workerService = new WorkerService(workerRepository);

        Instant originalRegisteredAt = Instant.parse("2026-01-01T00:00:00Z");
        WorkerEntity existing = new WorkerEntity();
        existing.setId("worker-1");
        existing.setCpuCapacity(2.0);
        existing.setMemoryCapacity(4096L);
        existing.setCpuAvailable(1.0);
        existing.setMemoryAvailable(1024L);
        existing.setStatus(WorkerStatus.HEALTHY);
        existing.setRegisteredAt(originalRegisteredAt);

        WorkerRegistrationRequest request = new WorkerRegistrationRequest();
        request.setCpuCapacity(8.0);
        request.setMemoryCapacity(16384L);

        when(workerRepository.findById("worker-1")).thenReturn(Optional.of(existing));
        when(workerRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        WorkerRegistrationResult result = workerService.registerWorker("worker-1", request);

        assertThat(existing.getCpuCapacity()).isEqualTo(8.0);
        assertThat(existing.getMemoryCapacity()).isEqualTo(16384L);
        assertThat(existing.getCpuAvailable()).isEqualTo(8.0);
        assertThat(existing.getMemoryAvailable()).isEqualTo(16384L);
        assertThat(existing.getStatus()).isEqualTo(WorkerStatus.REGISTERING);
        assertThat(existing.getRegisteredAt()).isEqualTo(originalRegisteredAt);

        assertThat(result.alreadyRegistered()).isTrue();
    }

    @Test
    void recordHeartbeat_withRegisteredWorker_updatesLastHeartbeatAndMarksHealthy() {
        WorkerService workerService = new WorkerService(workerRepository);

        WorkerEntity existing = new WorkerEntity();
        existing.setId("worker-1");
        existing.setCpuCapacity(4.0);
        existing.setMemoryCapacity(8192L);
        existing.setCpuAvailable(4.0);
        existing.setMemoryAvailable(8192L);
        existing.setStatus(WorkerStatus.REGISTERING);
        existing.setRegisteredAt(Instant.parse("2026-01-01T00:00:00Z"));

        when(workerRepository.findById("worker-1")).thenReturn(Optional.of(existing));
        when(workerRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        WorkerResponse response = workerService.recordHeartbeat("worker-1");

        assertThat(existing.getStatus()).isEqualTo(WorkerStatus.HEALTHY);
        assertThat(existing.getLastHeartbeat()).isNotNull();

        assertThat(response.getStatus()).isEqualTo(WorkerStatus.HEALTHY);
        assertThat(response.getLastHeartbeat()).isNotNull();
    }

    @Test
    void recordHeartbeat_withUnknownId_throwsWorkerNotFoundException() {
        WorkerService workerService = new WorkerService(workerRepository);

        when(workerRepository.findById("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> workerService.recordHeartbeat("unknown"))
                .isInstanceOf(WorkerNotFoundException.class);
    }
}
