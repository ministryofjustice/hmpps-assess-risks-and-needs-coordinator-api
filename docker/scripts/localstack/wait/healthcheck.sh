#!/usr/bin/env bash

# SQS
queues=$(awslocal sqs list-queues)
echo $queues | grep "coordinator-queue" || exit 1
echo $queues | grep "audit-queue" || exit 1
