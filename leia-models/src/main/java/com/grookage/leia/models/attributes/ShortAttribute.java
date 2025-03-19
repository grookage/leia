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
public class ShortAttribute extends SchemaAttribute {
    public ShortAttribute(final String name,
                          final boolean optional,
                          final Set<QualifierInfo> qualifiers) {
        super(DataType.SHORT, name, optional, qualifiers);
    }

    @Override
    public <T> T accept(SchemaAttributeAcceptor<T> attributeAcceptor) {
        return attributeAcceptor.accept(this);
    }
}
