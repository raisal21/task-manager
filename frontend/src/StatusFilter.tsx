import { STATUS_LABELS, TASK_STATUSES } from "./taskStatus";
import type { StatusFilterValue } from "./taskFilter";

interface StatusFilterProps {
  value: StatusFilterValue;
  onChange: (value: StatusFilterValue) => void;
}

const OPTIONS: readonly { value: StatusFilterValue; label: string; hint?: string }[] = [
  { value: "ALL", label: "All" },
  { value: "OPEN", label: "Open", hint: "To Do and In Progress" },
  ...TASK_STATUSES.map((status) => ({ value: status, label: STATUS_LABELS[status] })),
];

/**
 * One status is filtered by the server (A14): a change of the value makes a new read with ?status=.
 * "Open" is derived on the client from the list without a filter (see taskFilter.ts).
 * The radio buttons are native, so the keyboard and the screen reader work as for any radio group.
 * The CSS draws each one as a toggle button.
 */
export default function StatusFilter({ value, onChange }: StatusFilterProps) {
  return (
    <fieldset className="status-filter">
      <legend>Show</legend>
      <div className="filter-options">
        {OPTIONS.map((option) => (
          <label key={option.value} className="filter-option" title={option.hint}>
            <input
              type="radio"
              name="status-filter"
              value={option.value}
              checked={value === option.value}
              onChange={() => onChange(option.value)}
            />
            <span>
              {option.label}
              {option.hint !== undefined && (
                <span className="visually-hidden"> ({option.hint})</span>
              )}
            </span>
          </label>
        ))}
      </div>
    </fieldset>
  );
}
