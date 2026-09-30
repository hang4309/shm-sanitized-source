package com.example.shm.vendorsource.service;

import com.example.shm.vendorsource.dto.VendorReadingQueryDTO;
import com.example.shm.vendorsource.dto.VendorReadingUploadDTO;
import com.example.shm.vendorsource.vo.VendorReadingVO;

import java.util.List;

/**
 * Unified vendor source layer. The OS265 collector is the canonical
 * producer; legacy fiber endpoints delegate here so a record is only
 * ever persisted once, into the unified reading table.
 */
public interface VendorSourceService {

    /**
     * Canonical OS265 collector ingestion. sourceFile and sourceLine are
     * mandatory; records without source metadata are rejected.
     */
    void ingestCanonical(VendorReadingUploadDTO dto);

    /**
     * Legacy fiber compatibility ingestion. Missing source metadata is
     * filled with explicit compatibility markers; sourceType defaults to
     * OS265 only inside this adapter.
     */
    void ingestLegacyFiber(VendorReadingUploadDTO dto);

    VendorReadingVO getLatest(VendorReadingQueryDTO dto);

    List<VendorReadingVO> getHistory(VendorReadingQueryDTO dto);
}
