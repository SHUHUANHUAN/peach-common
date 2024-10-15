package com.peach.common.util;

import cn.hutool.core.lang.UUID;

public class IDGenerator {
    public static String id() {
        return MajorKeyFactory.instance.getMajorKey();
    }

    public static String UUID() {
        return UUID.randomUUID().toString().replaceAll("-", "");
    }


    private static class MajorKeyFactory {
        private static final long twepoch = 1288834974657L;
        private static final long workerIdBits = 5L;
        private static final long datacenterIdBits = 5L;
        private static final long maxWorkerId = 31L;
        private static final long maxDatacenterId = 31L;
        private static final long sequenceBits = 12L;
        private static final long workerIdShift = 12L;
        private static final long datacenterIdShift = 17L;
        private static final long timestampLeftShift = 22L;
        private static final long sequenceMask = 4095L;
        private static long lastTimestamp = -1L;
        private long sequence = 0L;
        private final long workerId;
        private final long datacenterId;
        private static final MajorKeyFactory instance = new MajorKeyFactory(1L, 1L);

        private MajorKeyFactory(long workerId, long datacenterId) {
            if ((workerId > 31L) || (workerId < 0L)) {
                throw new IllegalArgumentException("worker Id can't be greater than %d or less than 0");
            }
            if ((datacenterId > 31L) || (datacenterId < 0L)) {
                throw new IllegalArgumentException("datacenter Id can't be greater than %d or less than 0");
            }
            this.workerId = workerId;
            this.datacenterId = datacenterId;
        }

        public synchronized String getMajorKey() {
            long timestamp = timeGen();
            if (timestamp < lastTimestamp) {
                try {
                    throw new Exception("Clock moved backwards.  Refusing to generate id for " + (lastTimestamp - timestamp) + " milliseconds");
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            if (lastTimestamp == timestamp) {
                this.sequence = (this.sequence + 1L & 0xFFF);
                if (this.sequence == 0L) {
                    timestamp = tilNextMillis(lastTimestamp);
                }
            } else {
                this.sequence = 0L;
            }
            lastTimestamp = timestamp;
            long nextId = timestamp - 1288834974657L << 22 | this.datacenterId << 17 | this.workerId << 12 | this.sequence;
            return String.valueOf(nextId);
        }

        private long tilNextMillis(long lastTimestamp) {
            long timestamp = timeGen();
            while (timestamp <= lastTimestamp) {
                timestamp = timeGen();
            }
            return timestamp;
        }

        private long timeGen() {
            return System.currentTimeMillis();
        }
    }


}
