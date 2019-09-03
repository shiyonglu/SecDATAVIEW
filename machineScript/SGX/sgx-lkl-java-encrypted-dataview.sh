#!/bin/bash

kill -9 $(pidof sgx-lkl-run)
cp /home/ubuntu/sgxlkl-disk.img.enc /home/ubuntu/sgx-lkl/apps/jvm/helloworld-java/
BASE_DIR="/home/ubuntu/sgx-lkl"

ENCLAVE_HEAP_SIZE=${SGXLKL_HEAP:-209715200}
ENCLAVE_DEBUG_KEY=${SGXLKL_KEY:-${BASE_DIR}/build/config/enclave_debug.key}

SGX_LKL_RUN_EXEC=${BASE_DIR}/build/sgx-lkl-run
SGX_LKL_OPTIONS="\
  SGXLKL_STHREADS=${SGXLKL_STHREADS:-4} \
  SGXLKL_ETHREADS=${SGXLKL_ETHREADS:-4} \
  SGXLKL_HEAP=2048M \
  SGXLKL_TAP=sgxlkl_tap0 \
  SGXLKL_VERBOSE=1 \
  SGXLKL_KEY=${ENCLAVE_DEBUG_KEY} \
  SGXLKL_HD_KEY="${@:1}""

JRE_LIB_PATH="/opt/j2re-image/lib/amd64:/opt/j2re-image/lib/amd64/jli:/opt/j2re-image/lib/amd64/server"
DEFAULT_JRE_ARGS="\
  -Xms2000k \
  -Xmx1024m \
  -XX:InitialCodeCacheSize=4000k \
  -XX:ReservedCodeCacheSize=8000K \
  -XX:CompressedClassSpaceSize=8000K \
  -XX:+UseCompressedClassPointers \
  -XX:+UseMembar \
  -cp /home/ubuntu"

DISK_IMAGE="/home/ubuntu/sgx-lkl/apps/jvm/helloworld-java/sgxlkl-disk.img.enc"
JAVAARGS="-jar /home/ubuntu/sshd.jar"

env LD_LIBRARY_PATH=${JRE_LIB_PATH} ${SGX_LKL_OPTIONS} ${SGX_LKL_RUN_EXEC}  ${DISK_IMAGE} /opt/j2re-image/bin/java ${DEFAULT_JRE_ARGS} ${JAVAARGS}

