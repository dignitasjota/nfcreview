package com.reviewtap.device;

import com.reviewtap.device.DeviceDtos.DeviceResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin · Dispositivos")
@RestController
@RequestMapping("/api/admin/devices")
@RequiredArgsConstructor
public class AdminDeviceController {

    private final DeviceService deviceService;

    /** Todos los dispositivos de todos los negocios (ADMIN). */
    @GetMapping
    public List<DeviceResponse> list() {
        return deviceService.listAll();
    }
}
