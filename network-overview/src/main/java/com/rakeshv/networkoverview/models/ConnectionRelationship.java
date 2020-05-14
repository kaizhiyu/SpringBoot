package com.rakeshv.networkoverview.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.neo4j.ogm.annotation.EndNode;
import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.RelationshipEntity;
import org.neo4j.ogm.annotation.StartNode;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@Builder
@RelationshipEntity(type = "PORT")
public class ConnectionRelationship {
    @Id
    @GeneratedValue
    private Long id;

    private List<String> connections = new ArrayList<>();

    @StartNode
    private Equipment equipment;

    @EndNode
    private Interface anInterface;
}
