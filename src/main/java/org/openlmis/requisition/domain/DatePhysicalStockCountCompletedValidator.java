/*
 * This program is part of the OpenLMIS logistics management information system platform software.
 * Copyright © 2017 VillageReach
 *
 * This program is free software: you can redistribute it and/or modify it under the terms
 * of the GNU Affero General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details. You should have received a copy of
 * the GNU Affero General Public License along with this program. If not, see
 * http://www.gnu.org/licenses.  For additional information contact info@OpenLMIS.org.
 */

package org.openlmis.requisition.domain;

import static org.openlmis.requisition.domain.Requisition.DATE_PHYSICAL_STOCK_COUNT_COMPLETED;
import static org.openlmis.requisition.i18n.MessageKeys.ERROR_DATE_STOCK_COUNT_MISMATCH;

import lombok.AllArgsConstructor;
import org.openlmis.requisition.utils.Message;
import java.time.LocalDate;
import java.util.Map;
import java.util.Objects;

@AllArgsConstructor
class DatePhysicalStockCountCompletedValidator implements DomainValidator {
  private final DatePhysicalStockCountCompleted datePhysicalStockCountCompleted;
  private final Requisition requisitionToUpdate;
  private final LocalDate currentDate;
  private final boolean isDatePhysicalStockCountCompletedEnabled;

  public void validate(Map<String, Message> errors) {
    if (isDatePhysicalStockCountCompletedEnabled) {
      if (dateDifferAfterAuthorize()) {
        errors.put(DATE_PHYSICAL_STOCK_COUNT_COMPLETED,
            new Message(ERROR_DATE_STOCK_COUNT_MISMATCH));
      }
      if (datePhysicalStockCountCompleted != null) {
        datePhysicalStockCountCompleted.validateNotInFuture(errors, currentDate);
      }
    }
  }

  private boolean dateDifferAfterAuthorize() {
    return requisitionToUpdate.getStatus().isAuthorized()
        && Objects.equals(requisitionToUpdate.getDatePhysicalStockCountCompleted(),
        datePhysicalStockCountCompleted);
  }
}
