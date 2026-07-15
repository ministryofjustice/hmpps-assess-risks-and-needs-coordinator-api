#!/usr/bin/env bash

# SQS
queues=$(awslocal sqs list-queues)
echo $queues | grep "coordinator-queue" || exit 1
