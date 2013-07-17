/*
 * Copyright 2013 University of Washington
 *
 * Licensed under the Educational Community License, Version 1.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.opensource.org/licenses/ecl1.php
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package piecework.util;

import piecework.Constants;
import piecework.model.Constraint;
import piecework.model.Field;
import piecework.model.FormValue;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * @author James Renfro
 */
public class ConstraintUtil {

    public static boolean hasConstraint(String type, List<Constraint> constraints) {
        if (constraints != null && !constraints.isEmpty()) {
            for (Constraint constraint : constraints) {
                if (constraint.getType() != null && constraint.getType().equals(type))
                    return true;
            }
        }
        return false;
    }

    public static boolean evaluate(Map<String, Field> fieldMap, Map<String, FormValue> formValueMap, Constraint constraint) {
        String constraintName = constraint.getName();
        String constraintValue = constraint.getValue();
        Pattern pattern = Pattern.compile(constraintValue);

        boolean isSatisfied = false;

        Field constraintField = fieldMap != null ? fieldMap.get(constraintName) : null;
        FormValue formValue = formValueMap != null ? formValueMap.get(constraintName) : null;
        List<String> fieldValues = formValue != null ? formValue.getAllValues() : null;

        // Evaluate whether this particular item is satisfied
        if (constraintField != null && (fieldValues == null || fieldValues.isEmpty())) {
            String defaultFieldValue = constraintField.getDefaultValue();
            isSatisfied = defaultFieldValue != null && pattern.matcher(defaultFieldValue).matches();
        } else {
            for (String fieldValue : fieldValues) {
                isSatisfied = fieldValue != null && pattern.matcher(fieldValue).matches();
                if (!isSatisfied)
                    break;
            }
        }

        // If it is satisfied, then evaluate each of the 'and' constraints
        if (isSatisfied) {
            return checkAll(null, fieldMap, formValueMap, constraint.getAnd());
        } else {
            if (constraint.getOr() != null && !constraint.getOr().isEmpty())
                return checkAny(null, fieldMap, formValueMap, constraint.getOr());
        }


        return isSatisfied;
    }

    public static boolean checkAll(String type, Map<String, Field> fieldMap, Map<String, FormValue> formValueMap, List<Constraint> constraints) {
        if (constraints != null && !constraints.isEmpty()) {
            for (Constraint constraint : constraints) {
                if (type == null || constraint.getType() == null || constraint.getType().equals(type)) {
                    if (! evaluate(fieldMap, formValueMap, constraint))
                        return false;
                }
            }
        }
        return true;
    }

    public static boolean checkAny(String type, Map<String, Field> fieldMap, Map<String, FormValue> formValueMap, List<Constraint> constraints) {
        if (constraints != null && !constraints.isEmpty()) {
            for (Constraint constraint : constraints) {
                if (type == null || constraint.getType() == null || constraint.getType().equals(type)) {
                    if (evaluate(fieldMap, formValueMap, constraint))
                        return true;
                }
            }
            return false;
        }
        return true;
    }


//    public static boolean isSatisfied(String type, Map<String, Field> fieldMap, ManyMap<String, String> formValueMap, List<Constraint> constraints, boolean requireAll) {
//        if (constraints == null || constraints.isEmpty())
//            return true;
//
//        boolean constraintIsSatisfied = requireAll;
//        for (Constraint constraint : constraints) {
//            String constraintType = constraint.getType();
//
//            if (constraintType == null)
//                continue;
//
//            boolean satisfied = false;
//            if (constraintType.equals(type)) {
//                List<Constraint> andConstraints =
//
//
//
//                String constraintName = constraint.getName();
//                String constraintValue = constraint.getValue();
//                Pattern pattern = Pattern.compile(constraintValue);
//
//                Field constraintField = fieldMap.get(constraintName);
//
//                if (constraintField != null) {
//                    List<String> fieldValues = formValueMap != null ? formValueMap.get(constraintName) : null;
//                    if (fieldValues == null || fieldValues.isEmpty()) {
//                        String defaultFieldValue = constraintField.getDefaultValue();
//                        satisfied = defaultFieldValue != null && pattern.matcher(defaultFieldValue).matches();
//                    } else {
//                        for (String fieldValue : fieldValues) {
//                            satisfied = fieldValue != null && pattern.matcher(fieldValue).matches();
//                            if (!satisfied)
//                                break;
//                        }
//                    }
//                }
//            } else if (constraintType.equals(Constants.ConstraintTypes.AND)) {
//                satisfied = isSatisfied(type, fieldMap, formValueMap, constraint.getSubconstraints(), true);
//            } else if (constraintType.equals(Constants.ConstraintTypes.OR)) {
//                satisfied = isSatisfied(type, fieldMap, formValueMap, constraint.getSubconstraints(), false);
//            } else {
//                continue;
//            }
//
//            if (requireAll && !satisfied) {
//                constraintIsSatisfied = false;
//                break;
//            } else if (!requireAll && satisfied) {
//                constraintIsSatisfied = true;
//                break;
//            }
//        }
//
//        return constraintIsSatisfied;
//    }

}
