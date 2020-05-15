package com.rakeshv.networkoverview.services;

import com.rakeshv.networkoverview.models.ConnectionsCsv;
import com.rakeshv.networkoverview.models.Equipment;
import com.rakeshv.networkoverview.models.EquipmentCsv;
import com.rakeshv.networkoverview.models.ExecutionStatus;
import com.rakeshv.networkoverview.models.Interface;
import com.rakeshv.networkoverview.models.InterfaceCsv;
import com.rakeshv.networkoverview.repositories.EquipmentRepository;
import com.rakeshv.networkoverview.repositories.InterfaceRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class UploadService {
    @Autowired
    EquipmentRepository equipmentRepository;
    @Autowired
    InterfaceRepository interfaceRepository;

    public void saveEquipments(List<EquipmentCsv> equipmentList) {
        for (EquipmentCsv equipmentCsv : equipmentList) {
            Equipment tmp = equipmentRepository.findByName(equipmentCsv.getName());
            if (tmp != null) {
                continue;
            }
            Equipment equipment = Equipment.builder()
                    .name(equipmentCsv.getName())
                    .ipaddress(equipmentCsv.getIpAddress())
                    .model(equipmentCsv.getModel())
                    .type(equipmentCsv.getType()).build();

            equipmentRepository.save(equipment);
        }
    }

    public ExecutionStatus saveInterface(List<InterfaceCsv> interfaceList) {
        boolean success = true;
        String message = "";
        for (InterfaceCsv interfaceCsv : interfaceList) {
            Equipment equipment = equipmentRepository.findByName(interfaceCsv.getSwitchName());
            if (equipment == null) {
                log.error("Unable to equipment with name {}", interfaceCsv.getSwitchName());
                message = "Unable to find equipment with name {}" + interfaceCsv.getSwitchName();
                success = false;
                continue;
            }

            Interface tmp = interfaceRepository.findByNameAndSwitchName(interfaceCsv.getName(), interfaceCsv.getSwitchName());
            if (tmp != null) {
                continue;
            }
            Interface anInterface = Interface.builder()
                    .name(interfaceCsv.getName())
                    .ipAddress(interfaceCsv.getIpAddress())
                    .macAddress(interfaceCsv.getMacAddress())
                    .switchId(equipment.getId())
                    .build();

            interfaceRepository.save(anInterface);

            equipment.addInterface(anInterface);
            equipmentRepository.save(equipment);
        }


        return ExecutionStatus.builder()
                .message(message)
                .status(success)
                .httpStatus(success ? HttpStatus.OK : HttpStatus.NOT_FOUND).build();

    }

    public ExecutionStatus saveConnections(List<ConnectionsCsv> connectionsList) {
        boolean success = true;
        String message = "";
        for (ConnectionsCsv connection : connectionsList) {
            Equipment sourceNode = equipmentRepository.findByName(connection.getSourceNode());
            Equipment targetNode = equipmentRepository.findByName(connection.getTargetNode());

            if (sourceNode == null || targetNode == null) {
                log.error("Unable to find either source or target node. continuing");
                message = "Unable to find either source of target node";
                success = false;
                continue;
            }

            Interface sourcePort = interfaceRepository.findByNameAndSwitchId(
                    connection.getSourcePort(), sourceNode.getId()
            );
            Interface targetPort = interfaceRepository.findByNameAndSwitchId(
                    connection.getTargetPort(), targetNode.getId()
            );

            if (sourcePort == null || targetPort == null) {
                log.error("Unable to find either source or target port. Continuing");
                message = "Unable to find either source of target port";
                success = false;
                continue;
            }

            // create connections
            log.info("Creating connection between {}-{} and {}-{}",
                    sourceNode.getName(), sourcePort.getName(), targetNode.getName(), targetPort.getName());
            equipmentRepository.createEquipmentRelationship(sourceNode.getName(), sourcePort.getName(), sourcePort.getSwitchId());
            equipmentRepository.createEquipmentRelationship(targetNode.getName(), targetPort.getName(), targetPort.getSwitchId());

            targetPort.setNeighborPort(sourcePort.getName());
            targetPort.setNeighborHost(sourceNode.getName());
            interfaceRepository.save(targetPort);

            sourcePort.addInterface(targetPort);
            sourcePort.setNeighborPort(targetPort.getName());
            sourcePort.setNeighborHost(targetNode.getName());
            interfaceRepository.save(sourcePort);

        }

        return ExecutionStatus.builder()
                .message(message)
                .status(success)
                .httpStatus(success ? HttpStatus.OK : HttpStatus.NOT_FOUND).build();
    }
}
