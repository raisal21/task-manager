import type { TaskStatus } from "./api/types";
import { STATUS_LABELS, TASK_STATUSES } from "./taskStatus";

export type StatusFilterValue = "ALL" | TaskStatus;

interface StatusFilterProps {
  value: StatusFilterValue;
  onChange: (value: StatusFilterValue) => void;
}

const OPTIONS: readonly { value: StatusFilterValue; label: string }[] = [
  { value: "ALL", label: "All" },
  ...TASK_STATUSES.map((status) => ({ value: status, label: STATUS_LABELS[status] })),
];

/** The server does the filter (A14). A change of the value makes a new read with ?status=. */
export default function StatusFilter({ value, onChange }: StatusFilterProps) {
  return (
    <fieldset className="status-filter">
      <legend>Show</legend>
      {OPTIONS.map((option) => (
        <label key={option.value}>
          <input
            type="radio"
            name="status-filter"
            value={option.value}
            checked={value === option.value}
            onChange={() => onChange(option.value)}
          />
          {option.label}
        </label>
      ))}
    </fieldset>
  );
}
