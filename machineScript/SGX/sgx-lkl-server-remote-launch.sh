kill -9 $(pidof sgx-lkl-run)
sleep 10
#rm SecDATAVIEW-v3-img.enc	
#cp ~/Desktop/jouranlExpriments/SecDATAVIEW-v3-img.enc ~/SecDATAVIEW-v3-img.enc
SGXLKL_HEAP=2048M \
SGXLKL_WG_IP=10.0.4.1 \
SGXLKL_KEY="/home/ubuntu/sgx-lkl/build/config/enclave_debug.key" \
SGXLKL_VERBOSE=1 \
SGXLKL_TAP=sgxlkl_tap0 \
SGXLKL_REMOTE_CONFIG=1 \
SGXLKL_IAS_SPID=3BE0751FD63C9A745CEE85DD21111111 \
SGXLKL_REPORT_NONCE=10867864710722948371 \
SGXLKL_MMAP_FILES=Shared \
SGXLKL_STHREADS=4 \
SGXLKL_ETHREADS=4 \
SGXLKL_WG_PEERS='qq7HIXT/93cFFn/87qN1ACKYxmCC57WaG6NPTXwpISM=:10.0.4.2/32:172.30.18.187:56002' \
/home/ubuntu/sgx-lkl/build/sgx-lkl-run ./SecDATAVIEW-v3-img.enc
