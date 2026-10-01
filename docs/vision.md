# CloudForge

## Vision

CloudForge is a distributed job-execution platform that allows users to submit computational workloads and have them automatically scheduled and executed across a pool of worker nodes.

The system is designed as a practical exploration of distributed systems, focusing on job scheduling, resource management, fault tolerance, asynchronous communication, and observability.

## Core Goal

Build a system where:

1. Users submit jobs through an API.
2. Jobs are placed into a queue.
3. A scheduler selects an appropriate worker based on available resources and scheduling policies.
4. The worker executes the job in an isolated environment.
5. The platform tracks the job throughout its lifecycle.
6. Failures are detected and jobs can be retried or rescheduled.
7. The system provides enough metrics and data to evaluate its performance.

## What Makes CloudForge Interesting

CloudForge is not intended to be a simple CRUD application or a collection of microservices.

The primary engineering focus is the distributed scheduling system: how workloads are assigned to workers, how resources are tracked, how failures are handled, and how different scheduling strategies affect system performance.

The project should prioritize real engineering problems and measurable behavior over unnecessary complexity.

## Initial Scope

The first version will support:

* Job submission and tracking
* Worker registration and health monitoring
* Asynchronous job communication
* Resource-aware scheduling
* Containerized job execution
* Job retries and failure handling
* Persistent job and worker state
* System metrics and monitoring

Advanced features such as multiple scheduler instances, leader election, and more sophisticated scheduling algorithms may be added after the core system is stable.

## Out of Scope

CloudForge is not intended to become:

* A replacement for Kubernetes
* A production-grade cloud platform
* A full infrastructure-as-a-service provider
* An unnecessarily large collection of microservices

The goal is to build a relatively small but technically deep distributed system that can be understood, measured, tested, and explained end-to-end.
