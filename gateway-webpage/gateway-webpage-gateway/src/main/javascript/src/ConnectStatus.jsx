import React, { useState, useEffect } from 'react';
import Select from 'react-select';

const ConnectOverview = () => {
    const [options, setOptions] = useState([]);
    const [features, setFeatures] = useState([]);
    const [installDisabled, setInstallDisabled] = useState(true);

    useEffect(() => {
        // Fetch options from the endpoint when the component mounts
        const fetchOptions = () => {
            try {
                fetch(`/data/hce/activeFeatures`, {
                    method: 'GET',
                    credentials: 'same-origin',
                    headers: {
                        'Accept': 'application/json'
                    }
                })
                .then(response => response.json())
                .then(json => {
                    console.log("activeFeatures JSON Response: ");
                    console.log(json);
                    if(json.availableFeatures) {
                        console.log(json.availableFeatures);
                        const formattedOptions = json.availableFeatures.map(name => ({
                            value: name,
                            label: name
                        }));

                        setOptions(formattedOptions);
                    } else {
                        throw new Error("Invalid features array retrieved from config.modules");
                    }
                });
            } catch (error) {
                console.error('Error fetching options:', error);
            }
        };

        fetchOptions();
    }, []);

    const handleChange = (selectedFeatures) => {
        setFeatures(selectedFeatures || []);
        setInstallDisabled(selectedFeatures.length === 0); // Update button state based on selectedFeatures
    };

    const install = () => {
        console.log("Selected features: ");
        let params = features.map((a) => a.value);
        console.log(params);

        // Convert params array to a query string
        let queryString = `?params=${params.join(',')}`;

        fetch(`/data/hce/install/${queryString}`, {
            method: 'GET',
            credentials: 'same-origin',
            headers: {
                'Accept': 'application/json'
            }
        })
        .then(response => response.json())
        .then(json => {
            console.log("install JSON Response: ");
            console.log(json);
        });
    };

    const showConfirmation = () => {
        console.log("Install clicked");

        // Generate confirmation message
        let confirmationMessage = "Are you sure you want to install ";
        if (features.length === 1) {
            confirmationMessage += `${features[0].label}?`;
        } else if (features.length > 1) {
            const featureLabels = features.map(feature => feature.label);
            const lastFeature = featureLabels.pop(); // Remove last element from array

            confirmationMessage += featureLabels.join(', ') + ` and ${lastFeature}?`;
        } else {
            confirmationMessage += "these features?";
        }

        // Display confirmation dialog
        if (window.confirm(confirmationMessage)) {
            install();
            alert("Installed");
        } else {
            console.log("Install cancelled");
        }
    };

    return (
        <div>
            <Select
                options={options}
                onChange={handleChange}
                value={features}
                isMulti
            />
            <button
                style={{
                    padding: '10px 20px',
                    backgroundColor: installDisabled ? '#cccccc' : '#2199e8', // Grey if disabled
                    color: 'white',
                    border: 'none',
                    borderRadius: '5px',
                    cursor: installDisabled ? 'not-allowed' : 'pointer', // Change cursor if disabled
                    fontSize: '16px',
                    fontWeight: 'bold'
                }}
                onClick={showConfirmation}
                disabled={installDisabled} // Disable button when features are empty
            >
                Install
            </button>
        </div>
    );
};

export default ConnectOverview;