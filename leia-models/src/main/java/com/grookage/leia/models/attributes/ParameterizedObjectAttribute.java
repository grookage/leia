package com.grookage.leia.models.attributes;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.grookage.leia.models.qualifiers.QualifierInfo;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Set;

@EqualsAndHashCode(callSuper = true)
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor
public class ParameterizedObjectAttribute extends SchemaAttribute {
    private SchemaAttribute rawTypeAttribute;
    private List<SchemaAttribute> typeAttributes;

    public ParameterizedObjectAttribute(final String name,
                                        final boolean optional,
                                        final Set<QualifierInfo> qualifiers,
                                        final SchemaAttribute rawTypeAttribute,
                                        final List<SchemaAttribute> typeAttributes) {
        super(DataType.PARAMETERIZED, name, optional, qualifiers);
        this.rawTypeAttribute = rawTypeAttribute;
        this.typeAttributes = typeAttributes;
    }


    @Override
    public <T> T accept(SchemaAttributeAcceptor<T> attributeAcceptor) {
        return attributeAcceptor.accept(this);
    }
}
