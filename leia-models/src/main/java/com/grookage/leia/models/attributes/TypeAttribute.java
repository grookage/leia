package com.grookage.leia.models.attributes;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.grookage.leia.models.qualifiers.QualifierInfo;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.Set;

@EqualsAndHashCode(callSuper = true)
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor
/*
* For capturing Type variables (T, U, S etc.) inside a Generic Class
* */
public class TypeAttribute extends SchemaAttribute{
    public TypeAttribute(final String name,
                           final boolean optional,
                           final Set<QualifierInfo> qualifiers) {
        super(DataType.TYPE, name, optional, qualifiers);
    }

    @Override
    public <T> T accept(SchemaAttributeAcceptor<T> attributeAcceptor) {
        return attributeAcceptor.accept(this);
    }
}
